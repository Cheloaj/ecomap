-- =============================================================================
-- ECOMAP · PARCHE DE SEGURIDAD 02 — Datos de negocio
-- =============================================================================
-- Cierra el RLS del resto de las tablas. El parche 01 dejó `users` blindada;
-- este se ocupa de businesses, products, ofertas, calificaciones, quejas y los
-- datos privados del cliente (canasta, favoritos, historial, preferencias).
--
-- MODELO QUE SE APLICA
--   · Catálogo (negocios, productos, calificaciones): LECTURA abierta a usuarios
--     autenticados. Es información que la app muestra a todo el mundo, así que
--     esconderla no aportaría nada.
--   · ESCRITURA: solo el dueño. Y los campos que otorgan privilegios
--     (verification_status, moderation_status, is_active) los blinda un trigger,
--     porque una política RLS no puede restringir columnas por sí sola.
--   · ADMINISTRACIÓN: vía la función is_admin(), que exige user_role = 'admin'.
--
-- REQUISITO IMPORTANTE PARA EL PANEL WEB
--   EcoMapAdmin hoy usa la anon key SIN autenticarse. Después de este parche
--   deja de poder moderar: tendrá que iniciar sesión con una cuenta que tenga
--   user_role = 'admin'. Es el punto de la sección 9, al final del archivo.
--
-- ORDEN: ejecutar DESPUÉS del parche 01.
-- =============================================================================

SET search_path = public, extensions;

-- -----------------------------------------------------------------------------
-- 1. ¿Quién es administrador?
-- -----------------------------------------------------------------------------
-- SECURITY DEFINER a propósito: si la política consultara `users` directamente,
-- la propia RLS de esa tabla se evaluaría dentro de la política y provocaría
-- recursión infinita. Este es el patrón recomendado por Supabase.
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.users
        WHERE id = auth.uid() AND user_role = 'admin'
    );
$$;

GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated;

-- ¿Este negocio es mío?
CREATE OR REPLACE FUNCTION public.owns_business(business_uuid UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.businesses
        WHERE id = business_uuid AND user_id = auth.uid()
    );
$$;

GRANT EXECUTE ON FUNCTION public.owns_business(UUID) TO authenticated;


-- -----------------------------------------------------------------------------
-- 2. Limpiar las políticas permisivas anteriores
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    p RECORD;
BEGIN
    FOR p IN
        SELECT policyname, tablename FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename IN ('businesses','products','offers','scheduled_offers',
                            'product_ratings','product_complaints','product_reports',
                            'favorites','user_history','basket','user_preferences')
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', p.policyname, p.tablename);
    END LOOP;
END $$;


-- -----------------------------------------------------------------------------
-- 3. businesses
-- -----------------------------------------------------------------------------
ALTER TABLE businesses ENABLE ROW LEVEL SECURITY;

-- Leer: los negocios aprobados y activos los ve cualquiera autenticado (así
-- funciona el mapa de EcoMap Usuario); el dueño ve además los suyos en
-- cualquier estado, y el admin los ve todos.
CREATE POLICY "businesses_select" ON businesses
    FOR SELECT TO authenticated
    USING (
        (verification_status = 'approved' AND is_active = TRUE)
        OR user_id = auth.uid()
        OR public.is_admin()
    );

CREATE POLICY "businesses_insert_own" ON businesses
    FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "businesses_update_own" ON businesses
    FOR UPDATE TO authenticated
    USING (user_id = auth.uid() OR public.is_admin())
    WITH CHECK (user_id = auth.uid() OR public.is_admin());

CREATE POLICY "businesses_delete_own" ON businesses
    FOR DELETE TO authenticated
    USING (user_id = auth.uid() OR public.is_admin());

-- Blindaje de columnas: sin esto, un vendedor podría aprobarse su propio
-- negocio con un PATCH y saltarse por completo la verificación de documentos.
CREATE OR REPLACE FUNCTION public.protect_business_columns()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF auth.uid() IS NULL OR public.is_admin() THEN
        RETURN NEW;
    END IF;

    NEW.verification_status := OLD.verification_status;
    NEW.approved_at         := OLD.approved_at;
    NEW.user_id             := OLD.user_id;   -- no se puede regalar un negocio
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_protect_business_columns ON businesses;
CREATE TRIGGER trg_protect_business_columns
    BEFORE UPDATE ON businesses
    FOR EACH ROW EXECUTE FUNCTION public.protect_business_columns();


-- -----------------------------------------------------------------------------
-- 4. products
-- -----------------------------------------------------------------------------
ALTER TABLE products ENABLE ROW LEVEL SECURITY;

-- El catálogo es visible para cualquier usuario autenticado. Los productos
-- comunitarios pendientes de moderación solo los ve su dueño (y el admin).
CREATE POLICY "products_select" ON products
    FOR SELECT TO authenticated
    USING (
        moderation_status = 'approved'
        OR user_id = auth.uid()
        OR public.owns_business(business_id)
        OR public.is_admin()
    );

CREATE POLICY "products_insert_own" ON products
    FOR INSERT TO authenticated
    WITH CHECK (
        (business_id IS NOT NULL AND public.owns_business(business_id))
        OR (user_id IS NOT NULL AND user_id = auth.uid())
    );

CREATE POLICY "products_update_own" ON products
    FOR UPDATE TO authenticated
    USING (
        user_id = auth.uid() OR public.owns_business(business_id) OR public.is_admin()
    )
    WITH CHECK (
        user_id = auth.uid() OR public.owns_business(business_id) OR public.is_admin()
    );

CREATE POLICY "products_delete_own" ON products
    FOR DELETE TO authenticated
    USING (
        user_id = auth.uid() OR public.owns_business(business_id) OR public.is_admin()
    );

-- Blindaje: un producto COMUNITARIO no puede auto-aprobarse su moderación.
-- Los productos de negocio sí nacen aprobados: es una decisión de producto,
-- el vendedor ya pasó por verificación de documentos.
CREATE OR REPLACE FUNCTION public.protect_product_columns()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF auth.uid() IS NULL OR public.is_admin() THEN
        RETURN NEW;
    END IF;

    IF NEW.business_id IS NULL THEN
        NEW.moderation_status := OLD.moderation_status;
        NEW.approved_at       := OLD.approved_at;
        NEW.approved_by       := OLD.approved_by;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_protect_product_columns ON products;
CREATE TRIGGER trg_protect_product_columns
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION public.protect_product_columns();


-- -----------------------------------------------------------------------------
-- 5. offers y scheduled_offers  (solo del propio negocio)
-- -----------------------------------------------------------------------------
ALTER TABLE offers           ENABLE ROW LEVEL SECURITY;
ALTER TABLE scheduled_offers ENABLE ROW LEVEL SECURITY;

CREATE POLICY "offers_select" ON offers
    FOR SELECT TO authenticated
    USING (is_active = TRUE OR public.owns_business(business_id) OR public.is_admin());

CREATE POLICY "offers_write_own" ON offers
    FOR ALL TO authenticated
    USING (public.owns_business(business_id) OR public.is_admin())
    WITH CHECK (public.owns_business(business_id) OR public.is_admin());

CREATE POLICY "scheduled_offers_own" ON scheduled_offers
    FOR ALL TO authenticated
    USING (public.owns_business(business_id) OR public.is_admin())
    WITH CHECK (public.owns_business(business_id) OR public.is_admin());


-- -----------------------------------------------------------------------------
-- 6. product_ratings  (leer todas, escribir la propia)
-- -----------------------------------------------------------------------------
ALTER TABLE product_ratings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "ratings_select" ON product_ratings
    FOR SELECT TO authenticated USING (TRUE);

CREATE POLICY "ratings_insert_own" ON product_ratings
    FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());

-- El autor edita su reseña; el vendedor solo puede añadir su respuesta.
CREATE POLICY "ratings_update" ON product_ratings
    FOR UPDATE TO authenticated
    USING (
        user_id = auth.uid()
        OR public.is_admin()
        OR EXISTS (
            SELECT 1 FROM products p
            WHERE p.id = product_ratings.product_id
              AND public.owns_business(p.business_id)
        )
    );

CREATE POLICY "ratings_delete_own" ON product_ratings
    FOR DELETE TO authenticated
    USING (user_id = auth.uid() OR public.is_admin());


-- -----------------------------------------------------------------------------
-- 7. product_complaints  (usuario reporta -> admin aprueba -> vendedor responde)
-- -----------------------------------------------------------------------------
ALTER TABLE product_complaints ENABLE ROW LEVEL SECURITY;

-- El vendedor SOLO ve las quejas que el admin ya aprobó (admin_approved = true).
-- Antes esta tabla era de lectura abierta y cualquiera veía todas las quejas
-- de todos, aprobadas o no.
CREATE POLICY "complaints_select" ON product_complaints
    FOR SELECT TO authenticated
    USING (
        user_id = auth.uid()
        OR public.is_admin()
        OR (
            admin_approved = TRUE
            AND EXISTS (
                SELECT 1 FROM products p
                WHERE p.id = product_complaints.product_id
                  AND public.owns_business(p.business_id)
            )
        )
    );

CREATE POLICY "complaints_insert_own" ON product_complaints
    FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());

CREATE POLICY "complaints_update" ON product_complaints
    FOR UPDATE TO authenticated
    USING (
        user_id = auth.uid()
        OR public.is_admin()
        OR EXISTS (
            SELECT 1 FROM products p
            WHERE p.id = product_complaints.product_id
              AND public.owns_business(p.business_id)
        )
    );

CREATE POLICY "complaints_delete_own" ON product_complaints
    FOR DELETE TO authenticated
    USING (user_id = auth.uid() OR public.is_admin());

-- Blindaje: el vendedor no puede auto-aprobarse una queja para ocultarla,
-- ni cambiarle el estado. Eso es potestad del administrador.
CREATE OR REPLACE FUNCTION public.protect_complaint_columns()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF auth.uid() IS NULL OR public.is_admin() THEN
        RETURN NEW;
    END IF;

    NEW.admin_approved := OLD.admin_approved;
    NEW.user_id        := OLD.user_id;
    NEW.product_id     := OLD.product_id;

    -- Quien NO es el autor solo puede tocar los campos de respuesta del vendedor
    IF OLD.user_id <> auth.uid() THEN
        NEW.reason      := OLD.reason;
        NEW.description := OLD.description;
        NEW.image_url   := OLD.image_url;
        NEW.status      := OLD.status;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_protect_complaint_columns ON product_complaints;
CREATE TRIGGER trg_protect_complaint_columns
    BEFORE UPDATE ON product_complaints
    FOR EACH ROW EXECUTE FUNCTION public.protect_complaint_columns();


-- -----------------------------------------------------------------------------
-- 8. Datos privados del cliente
-- -----------------------------------------------------------------------------
-- Canasta, favoritos, historial y preferencias son datos personales: nadie más
-- que su dueño los ve. Antes eran legibles con la anon key.
ALTER TABLE product_reports  ENABLE ROW LEVEL SECURITY;
ALTER TABLE favorites        ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_history     ENABLE ROW LEVEL SECURITY;
ALTER TABLE basket           ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;

CREATE POLICY "product_reports_select" ON product_reports
    FOR SELECT TO authenticated
    USING (
        user_id = auth.uid()
        OR public.owns_business(business_id)
        OR public.is_admin()
    );

CREATE POLICY "product_reports_write_own" ON product_reports
    FOR ALL TO authenticated
    USING (user_id = auth.uid() OR public.is_admin())
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "favorites_own" ON favorites
    FOR ALL TO authenticated
    USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

CREATE POLICY "user_history_own" ON user_history
    FOR ALL TO authenticated
    USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

CREATE POLICY "basket_own" ON basket
    FOR ALL TO authenticated
    USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

CREATE POLICY "user_preferences_own" ON user_preferences
    FOR ALL TO authenticated
    USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());


-- -----------------------------------------------------------------------------
-- 9. Retirar el acceso de anon
-- -----------------------------------------------------------------------------
-- La anon key va dentro del APK: cualquiera la extrae. A partir de aquí, sin
-- iniciar sesión no se lee ni se escribe nada.
--
-- ⚠️ ESTO DEJA CIEGO AL PANEL EcoMapAdmin, que hoy usa la anon key sin
-- autenticarse. Para que vuelva a funcionar:
--   1. Crea una cuenta desde cualquiera de las apps.
--   2. Márcala como administrador:
--        UPDATE users SET user_role = 'admin' WHERE email = 'tu-correo@ejemplo.com';
--   3. Agrega una pantalla de login al panel (src/pages/login.jsx ya existe)
--      y usa supabase.auth.signInWithPassword antes de consultar.
REVOKE ALL ON public.businesses         FROM anon;
REVOKE ALL ON public.products           FROM anon;
REVOKE ALL ON public.offers             FROM anon;
REVOKE ALL ON public.scheduled_offers   FROM anon;
REVOKE ALL ON public.product_ratings    FROM anon;
REVOKE ALL ON public.product_complaints FROM anon;
REVOKE ALL ON public.product_reports    FROM anon;
REVOKE ALL ON public.favorites          FROM anon;
REVOKE ALL ON public.user_history       FROM anon;
REVOKE ALL ON public.basket             FROM anon;
REVOKE ALL ON public.user_preferences   FROM anon;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.businesses         TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.products           TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.offers             TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.scheduled_offers   TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.product_ratings    TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.product_complaints TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.product_reports    TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.favorites          TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_history       TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.basket             TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_preferences   TO authenticated;

-- La vista de estadísticas de calificaciones también se cierra a anon
REVOKE ALL ON public.product_rating_stats FROM anon;
GRANT  SELECT ON public.product_rating_stats TO authenticated;


-- =============================================================================
-- VERIFICACIÓN
-- =============================================================================
SELECT tablename, COUNT(*) AS politicas
FROM pg_policies WHERE schemaname = 'public'
GROUP BY tablename ORDER BY tablename;

-- Debe salir 0: ninguna tabla con RLS desactivado
SELECT relname AS tabla_sin_rls
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind = 'r' AND NOT c.relrowsecurity;


-- =============================================================================
-- LO QUE ESTE PARCHE **NO** CUBRE
-- =============================================================================
-- · El panel EcoMapAdmin queda sin acceso hasta que se le agregue login.
-- · No hay rate limiting por IP (Supabase lo ofrece en planes de pago).
-- · El bucket verification-documents debe marcarse como PRIVADO a mano en el
--   dashboard; el código ya no depende de que sea público.
-- · Los datos semilla de demo siguen existiendo: bórralos antes de cualquier
--   uso real.
-- =============================================================================
