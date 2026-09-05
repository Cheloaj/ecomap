-- =============================================================================
-- ECOMAP · init_demo_db.sql
-- Reconstrucción completa de la base de datos desde cero para demo en vivo.
-- =============================================================================
--
-- Consolida los 27 archivos .sql sueltos de EcoMapSocio/, EcoMapUsuario/ y
-- EcoMapAdmin/ en un solo script idempotente y ordenado.
--
-- IMPORTANTE: los .sql originales eran PARCHES incrementales (ALTER TABLE,
-- fixes de RLS, consultas de diagnóstico). NINGUNO contenía el CREATE TABLE de
-- users, businesses ni products. Esas definiciones se reconstruyeron a partir
-- de los modelos @Serializable de Kotlin, que son el contrato real que las apps
-- envían y esperan de PostgREST:
--   EcoMapSocio/app/src/main/java/com/ecomap/socio/data/model/*.kt
--   EcoMapUsuario/app/src/main/java/com/ecomap/usuario/data/model/*.kt
--
-- CÓMO EJECUTARLO
--   1. Supabase Dashboard > SQL Editor > New query
--   2. Pega este archivo COMPLETO y ejecútalo de una sola vez.
--   3. Crea los 5 buckets de Storage (ver SECCIÓN 10 al final).
--   4. Verifica Realtime (ver SECCIÓN 11 al final).
--
-- ⚠️ ESTE ARCHIVO ES PARA UNA BASE DE DEMO, NO PARA PRODUCCIÓN.
--    La SECCIÓN 8 (RLS) usa políticas permisivas a propósito, porque es lo que
--    el código actual de las apps necesita para funcionar sin bloquearse
--    durante la grabación. Lee la advertencia de esa sección antes de publicar.
--
-- ⚠️ DESTRUCTIVO: la SECCIÓN 1 hace DROP de todas las tablas públicas de EcoMap.
--    Como el proyecto de Supabase se borró y estás partiendo de cero, eso es lo
--    que quieres. Si lo corres contra una base con datos reales, los pierdes.
-- =============================================================================


-- =============================================================================
-- SECCIÓN 0 · EXTENSIONES
-- =============================================================================
-- pgcrypto: crypt() y gen_salt() — los usan reset_password_with_code y la
-- creación de las cuentas de demo en auth.users.
-- En Supabase las extensiones viven en el esquema "extensions", que NO está en
-- el search_path por defecto de todas las sesiones. Por eso se fija abajo.
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;

SET search_path = public, extensions;


-- =============================================================================
-- SECCIÓN 1 · LIMPIEZA (orden inverso a las dependencias)
-- =============================================================================
DROP VIEW IF EXISTS product_rating_stats CASCADE;
DROP VIEW IF EXISTS subscription_stats   CASCADE;

DROP TABLE IF EXISTS basket             CASCADE;
DROP TABLE IF EXISTS favorites          CASCADE;
DROP TABLE IF EXISTS user_history       CASCADE;
DROP TABLE IF EXISTS user_preferences   CASCADE;
DROP TABLE IF EXISTS product_ratings    CASCADE;
DROP TABLE IF EXISTS product_complaints CASCADE;
DROP TABLE IF EXISTS product_reports    CASCADE;
DROP TABLE IF EXISTS notifications      CASCADE;
DROP TABLE IF EXISTS scheduled_offers   CASCADE;
DROP TABLE IF EXISTS offers             CASCADE;
DROP TABLE IF EXISTS products           CASCADE;
DROP TABLE IF EXISTS businesses         CASCADE;
DROP TABLE IF EXISTS users              CASCADE;


-- =============================================================================
-- SECCIÓN 2 · TABLAS BASE
-- =============================================================================

-- -----------------------------------------------------------------------------
-- users  ·  espejo de auth.users en el esquema público
-- -----------------------------------------------------------------------------
-- NO existe trigger que cree esta fila: las apps la insertan a mano después de
-- signUpWith(Email), con id = auth.uid(). Ver AuthRepositoryImpl.kt:249.
--
-- OJO: email NO lleva UNIQUE a propósito. AuthRepositoryImpl de ambas apps busca
-- por email y luego filtra por user_type ('socio' vs 'cliente'), asumiendo que
-- un mismo correo puede tener dos filas. Un UNIQUE aquí rompería ese flujo.
CREATE TABLE users (
    id                       UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email                    TEXT        NOT NULL,
    full_name                TEXT        NOT NULL DEFAULT '',
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    email_verified           BOOLEAN     NOT NULL DEFAULT FALSE,
    verification_code        TEXT,
    verification_code_expiry TIMESTAMPTZ,
    account_status           TEXT        NOT NULL DEFAULT 'pending_verification',
    onboarding_step          TEXT        NOT NULL DEFAULT 'email_verification',
    onboarding_completed_at  TIMESTAMPTZ,
    fcm_token                TEXT,
    is_pro                   BOOLEAN     NOT NULL DEFAULT FALSE,
    user_type                TEXT        NOT NULL DEFAULT 'cliente',
    -- user_role lo usa EcoMapAdmin/setup_rls_policies.sql; el resto del código
    -- usa user_type. Se conservan ambas para no romper el panel web.
    user_role                TEXT        NOT NULL DEFAULT 'user',

    CONSTRAINT users_account_status_check
        CHECK (account_status IN ('pending_verification', 'active', 'suspended', 'rejected')),
    CONSTRAINT users_onboarding_step_check
        CHECK (onboarding_step IN ('email_verification', 'business_setup', 'document_upload', 'completed')),
    CONSTRAINT users_user_type_check
        CHECK (user_type IN ('socio', 'cliente', 'admin'))
);

CREATE INDEX idx_users_email     ON users(email);
CREATE INDEX idx_users_is_pro    ON users(is_pro);
CREATE INDEX idx_users_user_type ON users(user_type);


-- -----------------------------------------------------------------------------
-- businesses
-- -----------------------------------------------------------------------------
-- operating_hours es TEXT, NO jsonb. El modelo Kotlin lo declara como String
-- ("JSON stored as string"); si la columna fuera jsonb, PostgREST devolvería un
-- objeto y kotlinx-serialization fallaría al decodificar Business.
CREATE TABLE businesses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_name       TEXT        NOT NULL,
    business_type       TEXT        NOT NULL DEFAULT '',
    phone               TEXT,
    latitude            DOUBLE PRECISION NOT NULL DEFAULT 0,
    longitude           DOUBLE PRECISION NOT NULL DEFAULT 0,
    address             TEXT        NOT NULL DEFAULT '',
    operating_hours     TEXT        NOT NULL DEFAULT '',
    verification_status TEXT        NOT NULL DEFAULT 'pending',
    document_ine_url    TEXT,
    document_proof_url  TEXT,
    subscription_plan   TEXT        NOT NULL DEFAULT 'basic',
    avatar_url          TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    approved_at         TIMESTAMPTZ,
    approval_seen       BOOLEAN     NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    deactivated_at      TIMESTAMPTZ,

    CONSTRAINT businesses_verification_status_check
        CHECK (verification_status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT businesses_subscription_plan_check
        CHECK (subscription_plan IN ('basic', 'pro'))
);

CREATE INDEX idx_businesses_user_id  ON businesses(user_id);
CREATE INDEX idx_businesses_status   ON businesses(verification_status);
CREATE INDEX idx_businesses_active   ON businesses(is_active);
CREATE INDEX idx_businesses_location ON businesses(latitude, longitude);


-- =============================================================================
-- SECCIÓN 3 · TABLAS DEPENDIENTES
-- =============================================================================

-- -----------------------------------------------------------------------------
-- products  ·  productos de negocio Y productos comunitarios en la misma tabla
-- -----------------------------------------------------------------------------
-- Un producto tiene business_id (producto de negocio) O user_id (producto
-- comunitario), nunca ambos. Ver fix_business_id_constraint.sql.
CREATE TABLE products (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id         UUID REFERENCES businesses(id) ON DELETE CASCADE,
    user_id             UUID REFERENCES users(id) ON DELETE CASCADE,
    name                TEXT        NOT NULL,
    description         TEXT,
    price               DOUBLE PRECISION NOT NULL DEFAULT 0,
    image_url           TEXT,
    category            TEXT,
    unit                TEXT,
    is_available        BOOLEAN     NOT NULL DEFAULT TRUE,
    stock               INTEGER,

    -- Ubicación (solo productos comunitarios)
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    location_address    TEXT,

    -- Campos que solo declara el modelo de EcoMap Usuario
    category_name       TEXT,
    owner_name          TEXT,
    owner_phone         TEXT,

    -- Moderación (database_migration_part1.sql)
    moderation_status   TEXT        NOT NULL DEFAULT 'approved',
    approved_at         TIMESTAMPTZ,
    approved_by         UUID REFERENCES auth.users(id),

    -- Expiración automática de productos comunitarios (8 h)
    expires_at          TIMESTAMPTZ,

    -- Programación de publicación (feature PRO)
    publication_status  TEXT        NOT NULL DEFAULT 'published',
    scheduled_date      DATE,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Oferta
    is_on_offer         BOOLEAN     NOT NULL DEFAULT FALSE,
    original_price      DOUBLE PRECISION,
    offer_type          TEXT,
    discount_percentage INTEGER,
    offer_description   TEXT,
    offer_valid_until   TIMESTAMPTZ,

    CONSTRAINT products_moderation_status_check
        CHECK (moderation_status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT products_publication_status_check
        CHECK (publication_status IN ('scheduled', 'published')),
    CONSTRAINT products_must_have_owner
        CHECK (
            (business_id IS NOT NULL AND user_id IS NULL) OR
            (business_id IS NULL     AND user_id IS NOT NULL)
        )
);

CREATE INDEX idx_products_business_id       ON products(business_id);
CREATE INDEX idx_products_user_id           ON products(user_id);
CREATE INDEX idx_products_available         ON products(is_available);
CREATE INDEX idx_products_on_offer          ON products(is_on_offer);
CREATE INDEX idx_products_moderation_status ON products(moderation_status);
CREATE INDEX idx_products_expires_at        ON products(expires_at) WHERE expires_at IS NOT NULL;


-- -----------------------------------------------------------------------------
-- offers  ·  tabla legada
-- -----------------------------------------------------------------------------
-- Las ofertas hoy se derivan de products.is_on_offer (BusinessRepositoryImpl.kt:78),
-- pero EcoMap Socio todavía referencia from("offers") en 7 lugares y abre un canal
-- Realtime "offers-$businessId". Se conserva para que nada truene.
CREATE TABLE offers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id         UUID        NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    owner_id            UUID REFERENCES users(id) ON DELETE CASCADE,
    product_name        TEXT        NOT NULL,
    price               DOUBLE PRECISION NOT NULL DEFAULT 0,
    unit                TEXT        NOT NULL DEFAULT '',
    validity_type       TEXT        NOT NULL DEFAULT 'today',
    valid_until         TIMESTAMPTZ,
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    is_out_of_stock     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    view_count          INTEGER     NOT NULL DEFAULT 0,
    report_count        INTEGER     NOT NULL DEFAULT 0,
    confirmation_count  INTEGER     NOT NULL DEFAULT 0,
    title               TEXT,
    description         TEXT,
    discount_percentage INTEGER,
    original_price      DOUBLE PRECISION,
    discounted_price    DOUBLE PRECISION,
    start_date          TIMESTAMPTZ,
    end_date            TIMESTAMPTZ,
    image_url           TEXT
);

CREATE INDEX idx_offers_business_id ON offers(business_id);
CREATE INDEX idx_offers_active      ON offers(is_active);


-- -----------------------------------------------------------------------------
-- scheduled_offers  ·  ofertas programadas (feature PRO de Socio)
-- -----------------------------------------------------------------------------
-- status se serializa como el NOMBRE del enum Kotlin ScheduledOfferStatus,
-- en MAYÚSCULAS. No usar 'pending' en minúsculas aquí.
CREATE TABLE scheduled_offers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id         UUID        NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    product_name        TEXT        NOT NULL,
    price               DOUBLE PRECISION NOT NULL DEFAULT 0,
    unit                TEXT        NOT NULL DEFAULT 'kilogramo',
    validity_type       TEXT        NOT NULL DEFAULT 'today',
    scheduled_date      DATE        NOT NULL,
    scheduled_time      TEXT        NOT NULL DEFAULT '08:00',
    status              TEXT        NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at        TIMESTAMPTZ,
    published_offer_id  UUID,

    CONSTRAINT scheduled_offers_status_check
        CHECK (status IN ('PENDING', 'PUBLISHED', 'CANCELLED', 'FAILED'))
);

CREATE INDEX idx_scheduled_offers_business_id ON scheduled_offers(business_id);
CREATE INDEX idx_scheduled_offers_date        ON scheduled_offers(scheduled_date);


-- -----------------------------------------------------------------------------
-- notifications  ·  de SETUP_NOTIFICATIONS_FINAL.sql
-- -----------------------------------------------------------------------------
CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      TEXT        NOT NULL,
    body       TEXT        NOT NULL,
    type       TEXT        NOT NULL DEFAULT 'general',
    data       JSONB,
    read       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id    ON notifications(user_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_read       ON notifications(read);


-- -----------------------------------------------------------------------------
-- product_ratings  ·  de create_ratings_table.sql + respuesta del vendedor
-- -----------------------------------------------------------------------------
CREATE TABLE product_ratings (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id         UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    rating             INTEGER     NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment            TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Respuesta del vendedor (modelo de Socio)
    vendor_response    TEXT,
    vendor_response_at TIMESTAMPTZ,
    vendor_user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    -- Denormalizado para pintar la lista sin join
    user_name          TEXT,

    UNIQUE (user_id, product_id)
);

CREATE INDEX idx_product_ratings_product_id ON product_ratings(product_id);
CREATE INDEX idx_product_ratings_user_id    ON product_ratings(user_id);
CREATE INDEX idx_product_ratings_rating     ON product_ratings(rating);


-- -----------------------------------------------------------------------------
-- product_complaints  ·  quejas sobre un producto existente
-- -----------------------------------------------------------------------------
-- Flujo: Usuario reporta -> Admin revisa -> admin_approved = true -> el Socio lo ve.
CREATE TABLE product_complaints (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id         UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    reason             TEXT        NOT NULL,
    description        TEXT,
    image_url          TEXT,
    status             TEXT        NOT NULL DEFAULT 'pending',
    admin_approved     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    vendor_response    TEXT,
    vendor_response_at TIMESTAMPTZ,
    vendor_user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    -- Denormalizados que declaran los modelos
    user_name          TEXT,
    user_avatar_url    TEXT,
    product_name       TEXT,
    product_image_url  TEXT,

    CONSTRAINT product_complaints_status_check
        CHECK (status IN ('pending', 'reviewed', 'resolved', 'dismissed'))
);

COMMENT ON COLUMN product_complaints.admin_approved IS
'Indica si el administrador aprobó enviar este reporte al vendedor.
Los vendedores SOLO ven reportes con admin_approved = true.
Flujo: Usuario reporta -> Admin revisa -> Admin aprueba -> Socio lo ve';

CREATE INDEX idx_product_complaints_product_id ON product_complaints(product_id);
CREATE INDEX idx_product_complaints_user_id    ON product_complaints(user_id);
CREATE INDEX idx_product_complaints_status     ON product_complaints(status);
CREATE INDEX idx_product_complaints_admin_approved
    ON product_complaints(admin_approved) WHERE admin_approved = TRUE;


-- -----------------------------------------------------------------------------
-- product_reports  ·  "aquí falta este producto" (reporte de faltante, Usuario)
-- -----------------------------------------------------------------------------
CREATE TABLE product_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_id     UUID        NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    product_name    TEXT        NOT NULL,
    description     TEXT        NOT NULL DEFAULT '',
    estimated_price DOUBLE PRECISION,
    image_url       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '8 hours'),
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_product_reports_business_id ON product_reports(business_id);
CREATE INDEX idx_product_reports_user_id     ON product_reports(user_id);


-- -----------------------------------------------------------------------------
-- favorites / user_history / basket / user_preferences  (EcoMap Usuario)
-- -----------------------------------------------------------------------------
CREATE TABLE favorites (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_id UUID        NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, business_id)
);
CREATE INDEX idx_favorites_user_id ON favorites(user_id);

CREATE TABLE user_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_id UUID        NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    visited_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_history_user_id    ON user_history(user_id);
CREATE INDEX idx_user_history_visited_at ON user_history(visited_at DESC);

CREATE TABLE basket (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity   INTEGER     NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, product_id)
);
CREATE INDEX idx_basket_user_id ON basket(user_id);

-- Los valores por defecto de lat/lng son Ciudad del Carmen, Campeche,
-- tomados del modelo UserPreferences.kt.
CREATE TABLE user_preferences (
    user_id               UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name          TEXT,
    avatar_url            TEXT,
    default_latitude      DOUBLE PRECISION NOT NULL DEFAULT 18.6465,
    default_longitude     DOUBLE PRECISION NOT NULL DEFAULT -91.8323,
    notifications_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- =============================================================================
-- SECCIÓN 4 · VISTAS
-- =============================================================================

-- Consumida por EcoMap Usuario: from("product_rating_stats")
CREATE OR REPLACE VIEW product_rating_stats AS
SELECT
    product_id,
    COUNT(*)                                    AS total_ratings,
    AVG(rating)                                 AS average_rating,
    COUNT(CASE WHEN rating = 5 THEN 1 END)      AS five_stars,
    COUNT(CASE WHEN rating = 4 THEN 1 END)      AS four_stars,
    COUNT(CASE WHEN rating = 3 THEN 1 END)      AS three_stars,
    COUNT(CASE WHEN rating = 2 THEN 1 END)      AS two_stars,
    COUNT(CASE WHEN rating = 1 THEN 1 END)      AS one_star
FROM product_ratings
GROUP BY product_id;

-- Panel de administración
CREATE OR REPLACE VIEW subscription_stats AS
SELECT
    COUNT(*) FILTER (WHERE is_pro = TRUE)  AS total_pro_users,
    COUNT(*) FILTER (WHERE is_pro = FALSE) AS total_free_users,
    COUNT(*)                               AS total_users,
    ROUND(
        COUNT(*) FILTER (WHERE is_pro = TRUE)::numeric /
        NULLIF(COUNT(*), 0)::numeric * 100, 2
    ) AS pro_percentage
FROM users
WHERE user_type = 'cliente';


-- =============================================================================
-- SECCIÓN 5 · FUNCIONES DE TRIGGER
-- =============================================================================

-- 5.1 · Expiración automática de productos comunitarios (8 horas)
CREATE OR REPLACE FUNCTION set_product_expiration()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.user_id IS NOT NULL AND NEW.business_id IS NULL THEN
        NEW.expires_at := NOW() + INTERVAL '8 hours';
    ELSE
        NEW.expires_at := NULL;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trigger_set_product_expiration
    BEFORE INSERT ON products
    FOR EACH ROW
    EXECUTE FUNCTION set_product_expiration();


-- 5.2 · updated_at de product_ratings
CREATE OR REPLACE FUNCTION update_product_ratings_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trigger_update_product_ratings_updated_at
    BEFORE UPDATE ON product_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_product_ratings_updated_at();


-- 5.3 · Notificación automática al aprobar/rechazar una CUENTA
CREATE OR REPLACE FUNCTION send_account_status_notification()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    IF NEW.account_status = 'active' AND OLD.account_status <> 'active' THEN
        INSERT INTO notifications (user_id, title, body, type, data)
        VALUES (
            NEW.id,
            '¡Cuenta Aprobada! 🎉',
            'Tu negocio ha sido verificado. Ya puedes usar todas las funciones de EcoMap Socio.',
            'account_approved',
            jsonb_build_object('account_status', NEW.account_status, 'approved_at', NOW())
        );
    END IF;

    IF NEW.account_status = 'rejected' AND OLD.account_status <> 'rejected' THEN
        INSERT INTO notifications (user_id, title, body, type, data)
        VALUES (
            NEW.id,
            '❌ Cuenta Rechazada',
            'Tu solicitud de verificación ha sido rechazada. Por favor verifica tus documentos y vuelve a intentar.',
            'account_rejected',
            jsonb_build_object('account_status', NEW.account_status, 'rejected_at', NOW())
        );
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER on_account_status_change
    AFTER UPDATE OF account_status ON users
    FOR EACH ROW
    EXECUTE FUNCTION send_account_status_notification();


-- 5.4 · Notificación automática al aprobar/rechazar un NEGOCIO
CREATE OR REPLACE FUNCTION notify_business_status_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.verification_status = 'approved' THEN
        INSERT INTO notifications (user_id, title, body, type, read, created_at)
        VALUES (
            NEW.user_id,
            '¡Negocio Aprobado! 🎉',
            'Tu negocio "' || NEW.business_name || '" ha sido aprobado y ya está visible para tus clientes.',
            'business_approved', FALSE, NOW()
        );
    ELSIF NEW.verification_status = 'rejected' THEN
        INSERT INTO notifications (user_id, title, body, type, read, created_at)
        VALUES (
            NEW.user_id,
            'Negocio Rechazado',
            'Tu negocio "' || NEW.business_name || '" no cumplió con los requisitos de verificación. Contacta a soporte para más información.',
            'business_rejected', FALSE, NOW()
        );
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER business_status_notification
    AFTER UPDATE ON businesses
    FOR EACH ROW
    WHEN (OLD.verification_status IS DISTINCT FROM NEW.verification_status)
    EXECUTE FUNCTION notify_business_status_change();


-- 5.5 · Notificación al cambiar el estado PRO
-- CORRECCIÓN respecto al original: CONFIGURACION_SUPABASE_REALTIME.sql insertaba
-- en una columna "message" que NO existe en notifications (se llama "body"), así
-- que ese trigger reventaba al activar PRO desde la app. Aquí va corregido.
CREATE OR REPLACE FUNCTION notify_pro_status_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO notifications (user_id, title, body, type, created_at)
    VALUES (
        NEW.id,
        CASE WHEN NEW.is_pro THEN 'Suscripción Pro Activada' ELSE 'Suscripción Pro Desactivada' END,
        CASE WHEN NEW.is_pro
             THEN 'Ahora tienes acceso a todas las funciones Premium'
             ELSE 'Tu suscripción Pro ha sido desactivada' END,
        'subscription_change',
        NOW()
    );
    RETURN NEW;
END;
$$;

CREATE TRIGGER on_user_pro_status_change
    AFTER UPDATE ON users
    FOR EACH ROW
    WHEN (OLD.is_pro IS DISTINCT FROM NEW.is_pro)
    EXECUTE FUNCTION notify_pro_status_change();


-- =============================================================================
-- SECCIÓN 6 · FUNCIONES RPC  (las que las apps llaman con postgrest.rpc)
-- =============================================================================

-- 6.1 · update_verification_code — SOLUCION_RLS_VERIFICATION_CODE.sql
--       Llamada desde AuthRepositoryImpl.kt:408
CREATE OR REPLACE FUNCTION update_verification_code(
    user_id_param UUID,
    code_param    TEXT,
    expiry_param  TIMESTAMPTZ
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE users
    SET verification_code        = code_param,
        verification_code_expiry = expiry_param
    WHERE id = user_id_param;

    GET DIAGNOSTICS updated_count = ROW_COUNT;

    RETURN json_build_object('success', updated_count > 0, 'updated_count', updated_count);
END;
$$;

GRANT EXECUTE ON FUNCTION update_verification_code(UUID, TEXT, TIMESTAMPTZ) TO authenticated, anon;


-- 6.2 · reset_password_with_code — SOLUCION_RESET_PASSWORD.sql
--       Llamada desde AuthRepositoryImpl.kt:618
CREATE OR REPLACE FUNCTION reset_password_with_code(
    email_param        TEXT,
    code_param         TEXT,
    new_password_param TEXT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
-- search_path explícito: la función es SECURITY DEFINER y usa crypt()/gen_salt()
-- de pgcrypto, que vive en el esquema "extensions".
SET search_path = public, extensions, auth
AS $$
DECLARE
    user_id_var            UUID;
    verification_code_var  TEXT;
    expiry_var             TIMESTAMPTZ;
BEGIN
    SELECT id, verification_code, verification_code_expiry
      INTO user_id_var, verification_code_var, expiry_var
    FROM public.users
    WHERE email = email_param
    LIMIT 1;

    IF user_id_var IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Usuario no encontrado');
    END IF;

    IF verification_code_var IS DISTINCT FROM code_param THEN
        RETURN json_build_object('success', false, 'error', 'Código de verificación incorrecto');
    END IF;

    IF expiry_var IS NULL OR expiry_var < NOW() THEN
        RETURN json_build_object('success', false, 'error', 'Código de verificación expirado');
    END IF;

    UPDATE auth.users
    SET encrypted_password = crypt(new_password_param, gen_salt('bf')),
        updated_at         = NOW()
    WHERE id = user_id_var;

    UPDATE public.users
    SET verification_code        = NULL,
        verification_code_expiry = NULL
    WHERE id = user_id_var;

    RETURN json_build_object('success', true, 'message', 'Contraseña actualizada correctamente');

EXCEPTION
    WHEN OTHERS THEN
        RETURN json_build_object('success', false, 'error', SQLERRM);
END;
$$;

GRANT EXECUTE ON FUNCTION reset_password_with_code(TEXT, TEXT, TEXT) TO authenticated, anon;


-- 6.3 · delete_unverified_auth_user
--       Llamada desde AuthRepositoryImpl.kt:100 con el parámetro "user_email".
--       NO existía en ningún .sql del proyecto: se reconstruyó a partir del uso
--       en el código (borra el usuario "zombie" de auth.users cuando el registro
--       quedó a medias y hay que reintentarlo).
CREATE OR REPLACE FUNCTION delete_unverified_auth_user(user_email TEXT)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- Sólo borra cuentas que NO tengan una fila viva en public.users,
    -- para no eliminar por accidente a un usuario en uso.
    DELETE FROM auth.users a
    WHERE a.email = user_email
      AND NOT EXISTS (
          SELECT 1 FROM public.users p
          WHERE p.id = a.id AND p.email_verified = TRUE
      );

    GET DIAGNOSTICS deleted_count = ROW_COUNT;

    RETURN json_build_object('success', true, 'deleted_count', deleted_count);
EXCEPTION
    WHEN OTHERS THEN
        RETURN json_build_object('success', false, 'error', SQLERRM);
END;
$$;

GRANT EXECUTE ON FUNCTION delete_unverified_auth_user(TEXT) TO authenticated, anon;


-- 6.4 · publish_scheduled_products — PUBLICAR_PRODUCTOS_PROGRAMADOS.sql
CREATE OR REPLACE FUNCTION publish_scheduled_products()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE products
    SET publication_status = 'published',
        updated_at         = NOW()
    WHERE publication_status = 'scheduled'
      AND scheduled_date IS NOT NULL
      AND scheduled_date <= CURRENT_DATE;
END;
$$;

GRANT EXECUTE ON FUNCTION publish_scheduled_products() TO authenticated, anon;


-- 6.5 · Utilidades de notificaciones — SETUP_NOTIFICATIONS_FINAL.sql
CREATE OR REPLACE FUNCTION mark_notification_as_read(notification_id UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    UPDATE notifications SET read = TRUE
    WHERE id = notification_id AND user_id = auth.uid();
END;
$$;

CREATE OR REPLACE FUNCTION mark_all_notifications_as_read()
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    UPDATE notifications SET read = TRUE
    WHERE user_id = auth.uid() AND read = FALSE;
END;
$$;

CREATE OR REPLACE FUNCTION get_unread_notifications_count()
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN (SELECT COUNT(*) FROM notifications WHERE user_id = auth.uid() AND read = FALSE);
END;
$$;

GRANT EXECUTE ON FUNCTION mark_notification_as_read(UUID)        TO authenticated;
GRANT EXECUTE ON FUNCTION mark_all_notifications_as_read()        TO authenticated;
GRANT EXECUTE ON FUNCTION get_unread_notifications_count()        TO authenticated;


-- 6.6 · update_user_pro_status — panel admin
CREATE OR REPLACE FUNCTION update_user_pro_status(target_user_id UUID, new_is_pro BOOLEAN)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    current_is_pro BOOLEAN;
BEGIN
    SELECT is_pro INTO current_is_pro FROM public.users WHERE id = target_user_id;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Usuario no encontrado');
    END IF;

    IF current_is_pro = new_is_pro THEN
        RETURN json_build_object('success', true, 'changed', false);
    END IF;

    UPDATE public.users SET is_pro = new_is_pro, updated_at = NOW() WHERE id = target_user_id;

    RETURN json_build_object('success', true, 'changed', true,
                             'old_value', current_is_pro, 'new_value', new_is_pro);
END;
$$;

GRANT EXECUTE ON FUNCTION update_user_pro_status(UUID, BOOLEAN) TO authenticated;


-- =============================================================================
-- SECCIÓN 7 · PERMISOS DE ESQUEMA
-- =============================================================================
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES    IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;


-- =============================================================================
-- SECCIÓN 8 · ROW LEVEL SECURITY
-- =============================================================================
-- ⚠️⚠️ LEE ESTO ANTES DE PUBLICAR NADA ⚠️⚠️
--
-- Estas políticas son PERMISIVAS A PROPÓSITO. Reproducen lo que los .sql
-- originales ya hacían (fix_rls_para_panel.sql y fix_update_policies.sql
-- abrían lectura y escritura a 'anon') y es lo que el código actual necesita
-- para no bloquearse a media grabación: el panel web de admin no se autentica,
-- y el registro inserta en users antes de tener una sesión estable.
--
-- Con la anon key en el APK, esto significa que CUALQUIERA puede leer y
-- escribir estas tablas. Es aceptable para una base de demo desechable.
-- NO la uses para producción sin cerrar las políticas de escritura.
-- =============================================================================

ALTER TABLE users              ENABLE ROW LEVEL SECURITY;
ALTER TABLE businesses         ENABLE ROW LEVEL SECURITY;
ALTER TABLE products           ENABLE ROW LEVEL SECURITY;
ALTER TABLE offers             ENABLE ROW LEVEL SECURITY;
ALTER TABLE scheduled_offers   ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications      ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_ratings    ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_complaints ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_reports    ENABLE ROW LEVEL SECURITY;
ALTER TABLE favorites          ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_history       ENABLE ROW LEVEL SECURITY;
ALTER TABLE basket             ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences   ENABLE ROW LEVEL SECURITY;

-- 8.1 · users
CREATE POLICY "demo_users_select" ON users
    FOR SELECT TO authenticated, anon USING (TRUE);
CREATE POLICY "demo_users_insert" ON users
    FOR INSERT TO authenticated, anon WITH CHECK (TRUE);
CREATE POLICY "demo_users_update" ON users
    FOR UPDATE TO authenticated, anon USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "demo_users_delete" ON users
    FOR DELETE TO authenticated, anon USING (TRUE);

-- 8.2 · businesses
CREATE POLICY "demo_businesses_select" ON businesses
    FOR SELECT TO authenticated, anon USING (TRUE);
CREATE POLICY "demo_businesses_insert" ON businesses
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "demo_businesses_update" ON businesses
    FOR UPDATE TO authenticated, anon USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "demo_businesses_delete" ON businesses
    FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- 8.3 · products
CREATE POLICY "demo_products_select" ON products
    FOR SELECT TO authenticated, anon USING (TRUE);
CREATE POLICY "demo_products_insert" ON products
    FOR INSERT TO authenticated WITH CHECK (
        auth.uid() = user_id
        OR business_id IN (SELECT id FROM businesses WHERE user_id = auth.uid())
    );
CREATE POLICY "demo_products_update" ON products
    FOR UPDATE TO authenticated, anon USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "demo_products_delete" ON products
    FOR DELETE TO authenticated USING (
        auth.uid() = user_id
        OR business_id IN (SELECT id FROM businesses WHERE user_id = auth.uid())
    );

-- 8.4 · offers / scheduled_offers
CREATE POLICY "demo_offers_all" ON offers
    FOR ALL TO authenticated, anon USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "demo_scheduled_offers_all" ON scheduled_offers
    FOR ALL TO authenticated, anon USING (TRUE) WITH CHECK (TRUE);

-- 8.5 · notifications  (aquí sí conviene aislar por usuario: la app filtra por
--       user_id y Realtime entrega sólo lo que la política deja ver)
CREATE POLICY "demo_notifications_select" ON notifications
    FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "demo_notifications_insert" ON notifications
    FOR INSERT TO authenticated, anon WITH CHECK (TRUE);
CREATE POLICY "demo_notifications_update" ON notifications
    FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "demo_notifications_delete" ON notifications
    FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- 8.6 · ratings y quejas
CREATE POLICY "demo_ratings_select" ON product_ratings
    FOR SELECT TO authenticated, anon USING (TRUE);
CREATE POLICY "demo_ratings_write" ON product_ratings
    FOR ALL TO authenticated USING (auth.uid() = user_id OR auth.uid() = vendor_user_id)
    WITH CHECK (auth.uid() = user_id OR auth.uid() = vendor_user_id);

CREATE POLICY "demo_complaints_select" ON product_complaints
    FOR SELECT TO authenticated, anon USING (TRUE);
CREATE POLICY "demo_complaints_insert" ON product_complaints
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "demo_complaints_update" ON product_complaints
    FOR UPDATE TO authenticated, anon USING (TRUE) WITH CHECK (TRUE);
CREATE POLICY "demo_complaints_delete" ON product_complaints
    FOR DELETE TO authenticated USING (auth.uid() = user_id);

CREATE POLICY "demo_product_reports_all" ON product_reports
    FOR ALL TO authenticated, anon USING (TRUE) WITH CHECK (TRUE);

-- 8.7 · datos privados del cliente
CREATE POLICY "demo_favorites_all" ON favorites
    FOR ALL TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "demo_user_history_all" ON user_history
    FOR ALL TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "demo_basket_all" ON basket
    FOR ALL TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "demo_user_preferences_all" ON user_preferences
    FOR ALL TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);


-- =============================================================================
-- SECCIÓN 9 · REALTIME
-- =============================================================================
-- REPLICA IDENTITY FULL hace que los eventos UPDATE/DELETE incluyan la fila
-- anterior completa; sin esto, postgresChangeFlow recibe payloads incompletos.
ALTER TABLE users            REPLICA IDENTITY FULL;
ALTER TABLE businesses       REPLICA IDENTITY FULL;
ALTER TABLE products         REPLICA IDENTITY FULL;
ALTER TABLE notifications    REPLICA IDENTITY FULL;
ALTER TABLE offers           REPLICA IDENTITY FULL;
ALTER TABLE scheduled_offers REPLICA IDENTITY FULL;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['users', 'businesses', 'products', 'notifications', 'offers', 'scheduled_offers']
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = t
        ) THEN
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I', t);
        END IF;
    END LOOP;
END $$;


-- =============================================================================
-- SECCIÓN 10 · POLÍTICAS DE STORAGE
-- =============================================================================
-- Los buckets hay que CREARLOS A MANO en el Dashboard (ver la lista al final de
-- este archivo). Esto sólo abre los permisos de lectura/escritura sobre ellos.
DROP POLICY IF EXISTS "demo_storage_read"   ON storage.objects;
DROP POLICY IF EXISTS "demo_storage_insert" ON storage.objects;
DROP POLICY IF EXISTS "demo_storage_update" ON storage.objects;
DROP POLICY IF EXISTS "demo_storage_delete" ON storage.objects;

CREATE POLICY "demo_storage_read" ON storage.objects
    FOR SELECT TO authenticated, anon
    USING (bucket_id IN ('avatars', 'product-images', 'verification-documents',
                         'complaint-images', 'rating-images'));

CREATE POLICY "demo_storage_insert" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (bucket_id IN ('avatars', 'product-images', 'verification-documents',
                              'complaint-images', 'rating-images'));

CREATE POLICY "demo_storage_update" ON storage.objects
    FOR UPDATE TO authenticated
    USING (bucket_id IN ('avatars', 'product-images', 'verification-documents',
                         'complaint-images', 'rating-images'));

CREATE POLICY "demo_storage_delete" ON storage.objects
    FOR DELETE TO authenticated
    USING (bucket_id IN ('avatars', 'product-images', 'verification-documents',
                         'complaint-images', 'rating-images'));


-- =============================================================================
-- SECCIÓN 11 · DATOS SEMILLA (DEMO)
-- =============================================================================
-- Contenido: 2 cuentas, 2 negocios, 5 productos.
--
-- POR QUÉ SON DOS CUENTAS Y NO UNA:
-- Cada app rechaza al usuario de la otra. EcoMap Socio corta el login si
-- user_type = 'cliente' (AuthRepositoryImpl.kt:82) y EcoMap Usuario busca
-- explícitamente user_type = 'cliente'. Con una sola cuenta sólo podrías
-- grabar una de las dos apps.
--
-- CREDENCIALES
--   EcoMap Socio    ->  socio.demo@ecomap.mx     / EcoMap2026
--   EcoMap Usuario  ->  cliente.demo@ecomap.mx   / EcoMap2026
--
-- Las dos quedan con email_verified = TRUE, account_status = 'active',
-- onboarding_step = 'completed' e is_pro = TRUE, para que ninguna pantalla de
-- verificación, onboarding o muro PRO se interponga durante la grabación.
-- =============================================================================

-- 11.1 · Cuentas en auth.users (login real por email/contraseña)
DO $$
DECLARE
    v_socio_id   UUID := 'a0000000-0000-4000-8000-000000000001';
    v_cliente_id UUID := 'a0000000-0000-4000-8000-000000000002';
    v_has_provider_id BOOLEAN;
BEGIN
    -- Limpieza por si se re-ejecuta el script
    DELETE FROM auth.users WHERE id IN (v_socio_id, v_cliente_id);

    INSERT INTO auth.users (
        instance_id, id, aud, role, email, encrypted_password,
        email_confirmed_at, raw_app_meta_data, raw_user_meta_data,
        created_at, updated_at,
        confirmation_token, recovery_token, email_change, email_change_token_new
    )
    VALUES
    (
        '00000000-0000-0000-0000-000000000000', v_socio_id, 'authenticated', 'authenticated',
        'socio.demo@ecomap.mx', crypt('EcoMap2026', gen_salt('bf')),
        NOW(), '{"provider":"email","providers":["email"]}'::jsonb,
        '{"full_name":"Victor Hernández"}'::jsonb,
        NOW(), NOW(), '', '', '', ''
    ),
    (
        '00000000-0000-0000-0000-000000000000', v_cliente_id, 'authenticated', 'authenticated',
        'cliente.demo@ecomap.mx', crypt('EcoMap2026', gen_salt('bf')),
        NOW(), '{"provider":"email","providers":["email"]}'::jsonb,
        '{"full_name":"Ana Pérez"}'::jsonb,
        NOW(), NOW(), '', '', '', ''
    );

    -- auth.identities cambió de forma entre versiones de GoTrue: en las
    -- recientes lleva provider_id NOT NULL, en las viejas no existe.
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'auth' AND table_name = 'identities' AND column_name = 'provider_id'
    ) INTO v_has_provider_id;

    IF v_has_provider_id THEN
        EXECUTE format(
            'INSERT INTO auth.identities (id, user_id, provider_id, identity_data, provider,
                                          last_sign_in_at, created_at, updated_at)
             VALUES (gen_random_uuid(), %L, %L, %L::jsonb, ''email'', NOW(), NOW(), NOW()),
                    (gen_random_uuid(), %L, %L, %L::jsonb, ''email'', NOW(), NOW(), NOW())',
            v_socio_id,   v_socio_id::text,
            json_build_object('sub', v_socio_id::text,   'email', 'socio.demo@ecomap.mx')::text,
            v_cliente_id, v_cliente_id::text,
            json_build_object('sub', v_cliente_id::text, 'email', 'cliente.demo@ecomap.mx')::text
        );
    ELSE
        EXECUTE format(
            -- En el esquema viejo, identities.id ES el "subject" del proveedor
            -- (texto), no un uuid nuevo.
            'INSERT INTO auth.identities (id, user_id, identity_data, provider,
                                          last_sign_in_at, created_at, updated_at)
             VALUES (%L, %L, %L::jsonb, ''email'', NOW(), NOW(), NOW()),
                    (%L, %L, %L::jsonb, ''email'', NOW(), NOW(), NOW())',
            v_socio_id::text,   v_socio_id,
            json_build_object('sub', v_socio_id::text,   'email', 'socio.demo@ecomap.mx')::text,
            v_cliente_id::text, v_cliente_id,
            json_build_object('sub', v_cliente_id::text, 'email', 'cliente.demo@ecomap.mx')::text
        );
    END IF;
END $$;


-- 11.2 · Filas espejo en public.users
INSERT INTO users (
    id, email, full_name, email_verified, account_status,
    onboarding_step, onboarding_completed_at, is_pro, user_type, user_role
) VALUES
(
    'a0000000-0000-4000-8000-000000000001',
    'socio.demo@ecomap.mx', 'Victor Hernández',
    TRUE, 'active', 'completed', NOW(), TRUE, 'socio', 'user'
),
(
    'a0000000-0000-4000-8000-000000000002',
    'cliente.demo@ecomap.mx', 'Ana Pérez',
    TRUE, 'active', 'completed', NOW(), TRUE, 'cliente', 'user'
);


-- 11.3 · Dos negocios aprobados y activos en Ciudad del Carmen, Campeche
-- El horario va como TEXTO con el JSON exacto que espera OperatingHours.kt.
INSERT INTO businesses (
    id, user_id, business_name, business_type, phone,
    latitude, longitude, address, operating_hours,
    verification_status, subscription_plan, approved_at, approval_seen, is_active
) VALUES
(
    'b0000000-0000-4000-8000-000000000001',
    'a0000000-0000-4000-8000-000000000001',
    'Abarrotes La Esperanza',
    'abarrotes',
    '9381234567',
    18.6510, -91.8290,
    'Calle 31 #145, Col. Centro, Ciudad del Carmen, Campeche',
    '{"monday":{"is_open":true,"shifts":[{"open_time":"07:00","close_time":"21:00"}]},"tuesday":{"is_open":true,"shifts":[{"open_time":"07:00","close_time":"21:00"}]},"wednesday":{"is_open":true,"shifts":[{"open_time":"07:00","close_time":"21:00"}]},"thursday":{"is_open":true,"shifts":[{"open_time":"07:00","close_time":"21:00"}]},"friday":{"is_open":true,"shifts":[{"open_time":"07:00","close_time":"21:00"}]},"saturday":{"is_open":true,"shifts":[{"open_time":"07:00","close_time":"22:00"}]},"sunday":{"is_open":true,"shifts":[{"open_time":"08:00","close_time":"14:00"}]}}',
    'approved', 'pro', NOW(), TRUE, TRUE
),
(
    'b0000000-0000-4000-8000-000000000002',
    'a0000000-0000-4000-8000-000000000001',
    'Frutería El Buen Precio',
    'fruteria',
    '9387654321',
    18.6402, -91.8355,
    'Av. Concordia #88, Col. Justo Sierra, Ciudad del Carmen, Campeche',
    '{"monday":{"is_open":true,"shifts":[{"open_time":"06:30","close_time":"14:00"},{"open_time":"16:00","close_time":"20:00"}]},"tuesday":{"is_open":true,"shifts":[{"open_time":"06:30","close_time":"14:00"},{"open_time":"16:00","close_time":"20:00"}]},"wednesday":{"is_open":true,"shifts":[{"open_time":"06:30","close_time":"14:00"},{"open_time":"16:00","close_time":"20:00"}]},"thursday":{"is_open":true,"shifts":[{"open_time":"06:30","close_time":"14:00"},{"open_time":"16:00","close_time":"20:00"}]},"friday":{"is_open":true,"shifts":[{"open_time":"06:30","close_time":"14:00"},{"open_time":"16:00","close_time":"20:00"}]},"saturday":{"is_open":true,"shifts":[{"open_time":"06:30","close_time":"15:00"}]},"sunday":{"is_open":false,"shifts":[]}}',
    'approved', 'basic', NOW(), TRUE, TRUE
);


-- 11.4 · Cinco productos disponibles y aprobados
-- is_available = TRUE, moderation_status = 'approved' y publication_status =
-- 'published' son OBLIGATORIOS: son los tres filtros que aplican
-- BusinessRepositoryImpl y ProductRepositoryImpl antes de pintar la lista.
INSERT INTO products (
    id, business_id, name, description, price, category, unit,
    is_available, stock, moderation_status, publication_status,
    is_on_offer, original_price, offer_type, discount_percentage,
    offer_description, offer_valid_until
) VALUES
(
    'c0000000-0000-4000-8000-000000000001',
    'b0000000-0000-4000-8000-000000000001',
    'Arroz Súper Extra 1 kg',
    'Arroz blanco grano largo, bolsa de 1 kilogramo.',
    28.50, 'Abarrotes', 'pieza',
    TRUE, 40, 'approved', 'published',
    FALSE, NULL, NULL, NULL, NULL, NULL
),
(
    'c0000000-0000-4000-8000-000000000002',
    'b0000000-0000-4000-8000-000000000001',
    'Frijol Negro a granel',
    'Frijol negro de Campeche, precio por kilogramo.',
    36.00, 'Abarrotes', 'kilogramo',
    TRUE, 75, 'approved', 'published',
    FALSE, NULL, NULL, NULL, NULL, NULL
),
(
    'c0000000-0000-4000-8000-000000000003',
    'b0000000-0000-4000-8000-000000000001',
    'Aceite de maíz 1 L',
    'Aceite comestible de maíz, botella de 1 litro.',
    42.00, 'Abarrotes', 'litro',
    TRUE, 25, 'approved', 'published',
    TRUE, 52.00, 'discount', 20,
    '20% OFF sólo esta semana', NOW() + INTERVAL '7 days'
),
(
    'c0000000-0000-4000-8000-000000000004',
    'b0000000-0000-4000-8000-000000000002',
    'Plátano Tabasco',
    'Plátano de la región, maduro, precio por kilogramo.',
    18.00, 'Frutas', 'kilogramo',
    TRUE, 120, 'approved', 'published',
    FALSE, NULL, NULL, NULL, NULL, NULL
),
(
    'c0000000-0000-4000-8000-000000000005',
    'b0000000-0000-4000-8000-000000000002',
    'Naranja Valencia',
    'Naranja dulce para jugo, precio por kilogramo.',
    22.00, 'Frutas', 'kilogramo',
    TRUE, 90, 'approved', 'published',
    TRUE, 30.00, 'special_price', 27,
    'Lleva 3 kg por $60', NOW() + INTERVAL '3 days'
);


-- =============================================================================
-- SECCIÓN 12 · EXTRAS OPCIONALES PARA QUE LA APP NO SE VEA VACÍA
-- =============================================================================
-- Esto va MÁS ALLÁ de lo que pediste (2 negocios / 5 productos / usuario).
-- Si quieres exactamente el mínimo, no ejecutes esta sección: bórrala antes
-- de correr el script. Si la dejas, el mapa, las reseñas, la canasta y la
-- campana de notificaciones aparecen con contenido en el video.

-- 12.1 · Dos productos comunitarios (los que se ven como pines sueltos en el
--        mapa de EcoMap Usuario). El trigger les pone expires_at = ahora + 8 h,
--        pero NO desaparecen solos: quien los apaga es expire_community_products(),
--        y esa función no se ejecuta automáticamente (Supabase no tiene cron aquí).
--        Aun así, si grabas mañana conviene re-ejecutar sólo esta sección.
INSERT INTO products (
    id, user_id, name, description, price, category, unit,
    is_available, latitude, longitude, location_address,
    owner_name, moderation_status, publication_status
) VALUES
(
    'c0000000-0000-4000-8000-000000000006',
    'a0000000-0000-4000-8000-000000000002',
    'Limón agrio de traspatio',
    'Cosecha del día, vendo por kilo en la puerta de mi casa.',
    15.00, 'Frutas', 'kilogramo',
    TRUE, 18.6448, -91.8301,
    'Calle 24 s/n, Col. Renovación, Ciudad del Carmen',
    'Ana Pérez', 'approved', 'published'
),
(
    'c0000000-0000-4000-8000-000000000007',
    'a0000000-0000-4000-8000-000000000002',
    'Huevo de rancho (docena)',
    'Huevo fresco de gallina de rancho, por docena.',
    45.00, 'Abarrotes', 'pieza',
    TRUE, 18.6489, -91.8372,
    'Av. Isla de Tris #12, Ciudad del Carmen',
    'Ana Pérez', 'approved', 'published'
);

-- 12.2 · Reseñas del cliente sobre dos productos
INSERT INTO product_ratings (user_id, product_id, rating, comment, user_name) VALUES
('a0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000003',
 5, 'Excelente precio, es el más barato de la colonia.', 'Ana Pérez'),
('a0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000004',
 4, 'Buen plátano, aunque algunos venían muy maduros.', 'Ana Pérez');

-- 12.3 · Canasta del cliente con dos productos
INSERT INTO basket (user_id, product_id, quantity) VALUES
('a0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002', 2),
('a0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000005', 3);

-- 12.4 · Un favorito y una visita en el historial
INSERT INTO favorites (user_id, business_id) VALUES
('a0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000001');

INSERT INTO user_history (user_id, business_id) VALUES
('a0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000001');

-- 12.5 · Preferencias del cliente (centra el mapa en Ciudad del Carmen)
INSERT INTO user_preferences (user_id, display_name, default_latitude, default_longitude)
VALUES ('a0000000-0000-4000-8000-000000000002', 'Ana Pérez', 18.6465, -91.8323);

-- 12.6 · Notificaciones para que la campana no salga en cero
INSERT INTO notifications (user_id, title, body, type, read) VALUES
('a0000000-0000-4000-8000-000000000001',
 '¡Negocio Aprobado! 🎉',
 'Tu negocio "Abarrotes La Esperanza" ha sido aprobado y ya está visible para tus clientes.',
 'business_approved', FALSE),
('a0000000-0000-4000-8000-000000000001',
 'Nueva reseña en tu producto',
 'Ana P. calificó "Aceite de maíz 1 L" con 5 estrellas.',
 'general', FALSE),
('a0000000-0000-4000-8000-000000000002',
 'Suscripción Pro Activada',
 'Ahora tienes acceso a todas las funciones Premium',
 'subscription_change', FALSE);

-- 12.7 · Una oferta programada para mañana (pestaña "Programadas" de Socio)
INSERT INTO scheduled_offers (
    business_id, product_name, price, unit, validity_type,
    scheduled_date, scheduled_time, status
) VALUES (
    'b0000000-0000-4000-8000-000000000001',
    'Azúcar estándar 1 kg', 24.50, 'pieza', 'today',
    CURRENT_DATE + 1, '08:00', 'PENDING'
);


-- =============================================================================
-- SECCIÓN 13 · VERIFICACIÓN
-- =============================================================================
-- Corre esto después del script. Si algún conteo sale en 0, algo falló arriba.

SELECT 'users'             AS tabla, COUNT(*) AS filas FROM users
UNION ALL SELECT 'businesses',        COUNT(*) FROM businesses
UNION ALL SELECT 'products',          COUNT(*) FROM products
UNION ALL SELECT 'notifications',     COUNT(*) FROM notifications
UNION ALL SELECT 'product_ratings',   COUNT(*) FROM product_ratings
UNION ALL SELECT 'basket',            COUNT(*) FROM basket
UNION ALL SELECT 'scheduled_offers',  COUNT(*) FROM scheduled_offers
ORDER BY tabla;

-- Lo que verá EcoMap Usuario en el mapa (negocios aprobados y activos):
SELECT business_name, business_type, latitude, longitude, verification_status, is_active
FROM businesses
WHERE verification_status = 'approved' AND is_active = TRUE;

-- Lo que verá EcoMap Socio en su dashboard:
SELECT b.business_name, p.name, p.price, p.unit, p.is_available, p.is_on_offer
FROM products p
JOIN businesses b ON b.id = p.business_id
ORDER BY b.business_name, p.name;

-- Confirmar que las dos cuentas pueden iniciar sesión:
SELECT u.email, u.user_type, u.account_status, u.is_pro,
       (a.email_confirmed_at IS NOT NULL) AS email_confirmado,
       EXISTS (SELECT 1 FROM auth.identities i WHERE i.user_id = a.id) AS tiene_identity
FROM users u
JOIN auth.users a ON a.id = u.id
ORDER BY u.user_type;

-- Confirmar Realtime:
SELECT tablename FROM pg_publication_tables
WHERE pubname = 'supabase_realtime' AND schemaname = 'public'
ORDER BY tablename;


-- =============================================================================
-- SECCIÓN 14 · LO QUE FALTA HACER A MANO EN EL DASHBOARD
-- =============================================================================
--
-- A) BUCKETS DE STORAGE  (Dashboard > Storage > New bucket)
--    Los cinco deben quedar PÚBLICOS. Motivo: las cinco rutas de subida del
--    código llaman a bucket.publicUrl(...) y guardan esa URL en la base. En un
--    bucket privado, publicUrl() devuelve una URL que responde error y las
--    imágenes salen rotas.
--
--      1. avatars                 -> PÚBLICO   (fotos de perfil y de negocio)
--      2. product-images          -> PÚBLICO   (fotos de producto)
--      3. complaint-images        -> PÚBLICO   (evidencia de quejas)
--      4. rating-images           -> PÚBLICO   (fotos en reseñas)
--      5. verification-documents  -> PÚBLICO   (INE y comprobante de domicilio)
--
--    ⚠️ El número 5 es un problema real de privacidad, no un detalle: son
--    identificaciones oficiales quedando legibles para cualquiera que tenga la
--    URL. Para la demo de hoy déjalo público porque si no, el onboarding de
--    Socio se ve roto. Para producción hay que cambiar BusinessRepositoryImpl.kt:89
--    de publicUrl() a createSignedUrl() y volver el bucket privado.
--
-- B) REALTIME  (Dashboard > Database > Replication > supabase_realtime)
--    La SECCIÓN 9 ya las agrega por SQL. Verifica que aparezcan estas seis:
--
--      users             -> monitor de suscripción PRO en ambas apps
--      businesses        -> canales "businesses-$userId" y "businesses-changes"
--      products          -> canal "products-changes" (EcoMap Usuario)
--      notifications     -> canal "notifications-$userId" (EcoMap Socio)
--      offers            -> canal "offers-$businessId" (EcoMap Socio)
--      scheduled_offers  -> canal "scheduled-offers-$businessId" (EcoMap Socio)
--
-- C) AUTH  (Dashboard > Authentication > Providers > Email)
--    Desactiva "Confirm email". Las apps manejan su propio código de
--    verificación en users.verification_code; si dejas activada la confirmación
--    de Supabase, el registro de usuarios nuevos se queda sin sesión y el
--    INSERT en public.users falla.
--
-- D) EDGE FUNCTIONS (opcional, no bloquea el video)
--    Sólo si vas a grabar el correo de activación PRO:
--      send-pro-activation-email        -> el código está en
--                                          EcoMapUsuario/supabase/functions/
--      send-pro-activation-email-socio  -> este código NO existe en el disco,
--                                          sólo estaba desplegado en el proyecto
--                                          borrado. Habría que reescribirlo.
--    Ambas necesitan la variable de entorno RESEND_API_KEY.
--
-- E) local.properties de las dos apps
--    Actualiza supabase.url y supabase.key con los del proyecto NUEVO y vuelve
--    a compilar. Esos valores se inyectan en BuildConfig en tiempo de build:
--    si no recompilas, los APKs siguen apuntando al proyecto borrado.
--
-- =============================================================================
-- FIN
-- =============================================================================
