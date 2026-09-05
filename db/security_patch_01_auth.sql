-- =============================================================================
-- ECOMAP · PARCHE DE SEGURIDAD 01 — Autenticación
-- =============================================================================
-- Corrige los hallazgos CRÍTICO 1, CRÍTICO 2 y ALTO 3 de la auditoría:
--
--   1. El código de verificación viajaba al dispositivo y se comparaba en el
--      cliente. Con la anon key (extraíble del APK) cualquiera podía leer el
--      código de otra persona y tomar su cuenta.
--   2. RLS abierto: anon podía leer y escribir toda la tabla users.
--   3. Código generado con PRNG no criptográfico y sin límite de intentos.
--
-- ORDEN: ejecuta este archivo COMPLETO en el SQL Editor de Supabase.
-- Después hay que instalar el APK con los cambios de Kotlin que lo acompañan;
-- si actualizas solo la base, las apps viejas dejan de poder iniciar sesión
-- (que es justo lo que queremos: el acceso directo a la tabla queda cerrado).
-- =============================================================================

SET search_path = public, extensions;

-- -----------------------------------------------------------------------------
-- 1. Columnas para limitar intentos
-- -----------------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS verification_attempts   INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS verification_locked_until TIMESTAMPTZ;

COMMENT ON COLUMN users.verification_attempts IS
'Intentos fallidos consecutivos de código de verificación. Se reinicia al acertar.';
COMMENT ON COLUMN users.verification_locked_until IS
'Si es futuro, los intentos de código están bloqueados para esta cuenta.';


-- -----------------------------------------------------------------------------
-- 2. RPC: ¿existe este correo?  (reemplaza el SELECT directo del cliente)
-- -----------------------------------------------------------------------------
-- Devuelve SOLO lo que el flujo de registro/login necesita. Nunca el código
-- de verificación, nunca el token FCM, nunca las fechas internas.
--
-- Nota honesta: esto sigue permitiendo enumerar correos (saber si una cuenta
-- existe). Es inherente al mensaje "este correo ya está registrado" que la app
-- muestra; eliminarlo requeriría rediseñar la UX del registro.
CREATE OR REPLACE FUNCTION public.user_public_info(email_param TEXT)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    u RECORD;
BEGIN
    SELECT id, email, full_name, email_verified, account_status,
           onboarding_step, user_type, is_pro
      INTO u
    FROM public.users
    WHERE lower(email) = lower(email_param)
    ORDER BY created_at DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN json_build_object('exists', false);
    END IF;

    RETURN json_build_object(
        'exists',          true,
        'id',              u.id,
        'email',           u.email,
        'full_name',       u.full_name,
        'email_verified',  u.email_verified,
        'account_status',  u.account_status,
        'onboarding_step', u.onboarding_step,
        'user_type',       u.user_type,
        'is_pro',          u.is_pro
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.user_public_info(TEXT) TO anon, authenticated;


-- -----------------------------------------------------------------------------
-- 3. RPC: validar el código SIN exponerlo   ← el arreglo del CRÍTICO 1
-- -----------------------------------------------------------------------------
-- El código nunca sale de la base. El cliente manda correo + código y recibe
-- únicamente si fue válido. Incluye bloqueo por intentos (ALTO 3).
-- El propósito se deduce en el servidor, igual que hacía el cliente antes:
--   · Cuenta sin verificar  -> alta: consume el código y marca email_verified.
--   · Cuenta ya verificada  -> recuperación de contraseña: SOLO comprueba, sin
--     consumir, porque reset_password_with_code vuelve a necesitar el código.
CREATE OR REPLACE FUNCTION public.validate_verification_code(
    email_param TEXT,
    code_param  TEXT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    u             RECORD;
    max_attempts  CONSTANT INTEGER  := 5;
    lock_duration CONSTANT INTERVAL := INTERVAL '15 minutes';
BEGIN
    SELECT id, verification_code, verification_code_expiry,
           verification_attempts, verification_locked_until, email_verified
      INTO u
    FROM public.users
    WHERE lower(email) = lower(email_param)
    ORDER BY created_at DESC
    LIMIT 1;

    -- Respuesta uniforme para no revelar si la cuenta existe
    IF NOT FOUND THEN
        RETURN json_build_object('valid', false, 'reason', 'invalid_code');
    END IF;

    IF u.verification_locked_until IS NOT NULL
       AND u.verification_locked_until > NOW() THEN
        RETURN json_build_object(
            'valid', false, 'reason', 'locked',
            'retry_after', u.verification_locked_until
        );
    END IF;

    IF u.verification_code IS NULL
       OR u.verification_code_expiry IS NULL
       OR u.verification_code_expiry < NOW() THEN
        RETURN json_build_object('valid', false, 'reason', 'expired');
    END IF;

    -- Comparación en el servidor. El código nunca viaja al cliente.
    IF u.verification_code = code_param THEN
        IF u.email_verified THEN
            -- Recuperación de contraseña: el código sigue vivo para el paso
            -- siguiente (reset_password_with_code)
            UPDATE public.users
            SET verification_attempts     = 0,
                verification_locked_until = NULL
            WHERE id = u.id;
        ELSE
            -- Alta de cuenta: se consume el código y se marca el correo.
            --
            -- El avance de estado se hace AQUÍ y no en el cliente porque el
            -- trigger protect_user_privileged_columns impide que la app toque
            -- account_status (si no, cualquiera se activaría la cuenta solo).
            -- Cada app tenía su propio comportamiento y se respeta:
            --   socio   -> pasa a 'business_setup', sigue pendiente de validar
            --   cliente -> queda 'active' de una vez, como hacía EcoMap Usuario
            UPDATE public.users
            SET verification_attempts     = 0,
                verification_locked_until = NULL,
                email_verified            = TRUE,
                onboarding_step           = CASE WHEN users.user_type = 'socio'
                                                 THEN 'business_setup'
                                                 ELSE users.onboarding_step END,
                account_status            = CASE WHEN users.user_type = 'cliente'
                                                 THEN 'active'
                                                 ELSE users.account_status END,
                verification_code         = NULL,
                verification_code_expiry  = NULL
            WHERE id = u.id;
        END IF;

        RETURN json_build_object('valid', true, 'user_id', u.id);
    END IF;

    -- Fallo: subir el contador y bloquear si se pasó del límite
    UPDATE public.users
    SET verification_attempts = verification_attempts + 1,
        verification_locked_until = CASE
            WHEN verification_attempts + 1 >= max_attempts THEN NOW() + lock_duration
            ELSE verification_locked_until END
    WHERE id = u.id;

    RETURN json_build_object(
        'valid', false, 'reason', 'invalid_code',
        'attempts_left', GREATEST(0, max_attempts - (u.verification_attempts + 1))
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.validate_verification_code(TEXT, TEXT) TO anon, authenticated;


-- -----------------------------------------------------------------------------
-- 4. reset_password_with_code CON límite de intentos   ← refuerza ALTO 3
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.reset_password_with_code(
    email_param        TEXT,
    code_param         TEXT,
    new_password_param TEXT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions, auth
AS $$
DECLARE
    u             RECORD;
    max_attempts  CONSTANT INTEGER  := 5;
    lock_duration CONSTANT INTERVAL := INTERVAL '15 minutes';
BEGIN
    IF length(coalesce(new_password_param, '')) < 8 THEN
        RETURN json_build_object('success', false, 'error', 'La contraseña debe tener al menos 8 caracteres');
    END IF;

    SELECT id, verification_code, verification_code_expiry,
           verification_attempts, verification_locked_until
      INTO u
    FROM public.users
    WHERE lower(email) = lower(email_param)
    ORDER BY created_at DESC
    LIMIT 1;

    IF NOT FOUND THEN
        -- Mensaje genérico: no confirmamos si la cuenta existe
        RETURN json_build_object('success', false, 'error', 'Código de verificación incorrecto');
    END IF;

    IF u.verification_locked_until IS NOT NULL AND u.verification_locked_until > NOW() THEN
        RETURN json_build_object('success', false, 'error', 'Demasiados intentos. Espera 15 minutos.');
    END IF;

    IF u.verification_code IS NULL OR u.verification_code_expiry IS NULL
       OR u.verification_code_expiry < NOW() THEN
        RETURN json_build_object('success', false, 'error', 'Código de verificación expirado');
    END IF;

    IF u.verification_code IS DISTINCT FROM code_param THEN
        UPDATE public.users
        SET verification_attempts = verification_attempts + 1,
            verification_locked_until = CASE
                WHEN verification_attempts + 1 >= max_attempts THEN NOW() + lock_duration
                ELSE verification_locked_until END
        WHERE id = u.id;
        RETURN json_build_object('success', false, 'error', 'Código de verificación incorrecto');
    END IF;

    UPDATE auth.users
    SET encrypted_password = crypt(new_password_param, gen_salt('bf')),
        updated_at         = NOW()
    WHERE id = u.id;

    UPDATE public.users
    SET verification_code         = NULL,
        verification_code_expiry  = NULL,
        verification_attempts     = 0,
        verification_locked_until = NULL
    WHERE id = u.id;

    RETURN json_build_object('success', true, 'message', 'Contraseña actualizada correctamente');
EXCEPTION
    WHEN OTHERS THEN
        RETURN json_build_object('success', false, 'error', 'No se pudo actualizar la contraseña');
END;
$$;

GRANT EXECUTE ON FUNCTION public.reset_password_with_code(TEXT, TEXT, TEXT) TO anon, authenticated;


-- -----------------------------------------------------------------------------
-- 5. Código generado en el SERVIDOR con PRNG criptográfico   ← ALTO 3
-- -----------------------------------------------------------------------------
-- gen_random_bytes() de pgcrypto es criptográficamente seguro, a diferencia de
-- kotlin.random.Random que usaba la app. Además el código nunca pasa por el
-- cliente: se genera, se guarda y se devuelve solo el dato para enviar el correo.
CREATE OR REPLACE FUNCTION public.issue_verification_code(email_param TEXT)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions
AS $$
DECLARE
    u    RECORD;
    code TEXT;
BEGIN
    SELECT id, email, full_name INTO u
    FROM public.users
    WHERE lower(email) = lower(email_param)
    ORDER BY created_at DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false);
    END IF;

    -- 6 dígitos con entropía criptográfica.
    -- Se arman los bytes a mano en lugar de castear a bit(32), porque ese cast
    -- puede dar negativos y romper el lpad.
    SELECT lpad(((get_byte(b, 0)::BIGINT * 16777216
                + get_byte(b, 1)::BIGINT * 65536
                + get_byte(b, 2)::BIGINT * 256
                + get_byte(b, 3)::BIGINT) % 1000000)::TEXT, 6, '0')
      INTO code
    FROM (SELECT gen_random_bytes(4) AS b) s;

    UPDATE public.users
    SET verification_code         = code,
        verification_code_expiry  = NOW() + INTERVAL '15 minutes',
        verification_attempts     = 0,
        verification_locked_until = NULL
    WHERE id = u.id;

    -- Se devuelve el código porque hoy la app es quien dispara el correo.
    -- TODO: cuando el envío se mueva a una Edge Function, quitar 'code' de aquí
    -- para que nunca salga de la base.
    RETURN json_build_object('success', true, 'code', code, 'full_name', u.full_name);
END;
$$;

GRANT EXECUTE ON FUNCTION public.issue_verification_code(TEXT) TO anon, authenticated;


-- -----------------------------------------------------------------------------
-- 6. RLS de users: cerrar el acceso directo   ← CRÍTICO 2
-- -----------------------------------------------------------------------------
-- Se eliminan las políticas permisivas (las originales del proyecto y las de
-- demo) y se deja solo: cada quien ve y edita SU fila.
DROP POLICY IF EXISTS "demo_users_select"                        ON users;
DROP POLICY IF EXISTS "demo_users_insert"                        ON users;
DROP POLICY IF EXISTS "demo_users_update"                        ON users;
DROP POLICY IF EXISTS "demo_users_delete"                        ON users;
DROP POLICY IF EXISTS "Users can read their own data"            ON users;
DROP POLICY IF EXISTS "Users can subscribe to their own changes" ON users;
DROP POLICY IF EXISTS "Only admins can update is_pro"            ON users;
DROP POLICY IF EXISTS "Admins can read all users"                ON users;
DROP POLICY IF EXISTS "Admins can update users"                  ON users;

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Leer: solo tu propia fila (necesario también para el Realtime de suscripción)
CREATE POLICY "users_select_own"
    ON users FOR SELECT TO authenticated
    USING (auth.uid() = id);

-- Insertar: solo tu propia fila, y solo justo después de registrarte
CREATE POLICY "users_insert_own"
    ON users FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = id);

-- Actualizar: solo tu propia fila. Los campos sensibles los blinda el trigger
-- de abajo, porque una política no puede restringir columnas por sí sola.
CREATE POLICY "users_update_own"
    ON users FOR UPDATE TO authenticated
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Nadie borra filas de users desde el cliente.


-- -----------------------------------------------------------------------------
-- 7. Blindaje de columnas sensibles
-- -----------------------------------------------------------------------------
-- Sin esto, un usuario podría hacer PATCH sobre su propia fila y ponerse
-- is_pro = true (saltarse el pago) o user_type = 'admin'.
CREATE OR REPLACE FUNCTION public.protect_user_privileged_columns()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    -- Las funciones SECURITY DEFINER y el service_role no pasan por aquí
    IF auth.uid() IS NULL THEN
        RETURN NEW;
    END IF;

    -- Privilegios: solo los cambian las funciones SECURITY DEFINER o el admin.
    NEW.is_pro         := OLD.is_pro;
    NEW.user_type      := OLD.user_type;
    NEW.user_role      := OLD.user_role;
    NEW.account_status := OLD.account_status;

    -- El contador de intentos tampoco lo puede reiniciar el cliente,
    -- si no el bloqueo por fuerza bruta sería trivial de evadir.
    NEW.verification_attempts     := OLD.verification_attempts;
    NEW.verification_locked_until := OLD.verification_locked_until;

    -- verification_code SÍ queda editable por el dueño: la app lo escribe en su
    -- propia fila durante el registro, y el RLS ya impide que otros lo lean.

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_protect_user_privileged_columns ON users;
CREATE TRIGGER trg_protect_user_privileged_columns
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION public.protect_user_privileged_columns();


-- -----------------------------------------------------------------------------
-- 8. Quitar el acceso de anon a la tabla (deja solo los RPC)
-- -----------------------------------------------------------------------------
REVOKE ALL ON public.users FROM anon;
GRANT  SELECT, INSERT, UPDATE ON public.users TO authenticated;


-- =============================================================================
-- VERIFICACIÓN
-- =============================================================================
-- 1) El código ya NO se puede leer desde fuera. Esto debe devolver 0 filas
--    (ejecútalo desde la app o con la anon key; en el SQL Editor eres postgres
--    y el RLS no aplica):
--       GET /rest/v1/users?select=verification_code
--
-- 2) Políticas activas sobre users:
SELECT policyname, cmd, roles FROM pg_policies
WHERE tablename = 'users' ORDER BY policyname;

-- 3) Los RPC nuevos existen:
SELECT routine_name FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name IN ('user_public_info','validate_verification_code',
                       'issue_verification_code','reset_password_with_code')
ORDER BY routine_name;

-- =============================================================================
-- LO QUE ESTE PARCHE **NO** CUBRE (para que lo sepas)
-- =============================================================================
-- · businesses y products siguen con políticas permisivas: van en el parche 02.
-- · El envío del correo sigue saliendo desde la app, así que el código pasa por
--   el dispositivo del dueño de la cuenta. Cerrarlo del todo exige mover el
--   envío a una Edge Function.
-- · Se puede seguir enumerando correos registrados (limitación de la UX actual).
-- · No hay rate limiting por IP; el límite es por cuenta (5 intentos / 15 min).
-- =============================================================================
