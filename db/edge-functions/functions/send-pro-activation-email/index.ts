// Supabase Edge Function: send-pro-activation-email
// Envía email de confirmación cuando un usuario activa PRO

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const RESEND_API_KEY = Deno.env.get('RESEND_API_KEY')

interface EmailRequest {
  email: string
  userName: string
  planName: string
  activationDate: string
}

serve(async (req) => {
  // Verificar CORS
  if (req.method === 'OPTIONS') {
    return new Response('ok', {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
      }
    })
  }

  try {
    const { email, userName, planName, activationDate }: EmailRequest = await req.json()

    // Validar datos
    if (!email || !userName) {
      return new Response(
        JSON.stringify({ error: 'Email y nombre son requeridos' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Enviar email con Resend
    const res = await fetch('https://api.resend.com/emails', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${RESEND_API_KEY}`
      },
      body: JSON.stringify({
        from: 'EcoMap <onboarding@resend.dev>', // Cambiar por tu dominio verificado
        to: [email],
        subject: '🎉 ¡Bienvenido a EcoMap PRO!',
        html: `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Bienvenido a EcoMap PRO</title>
</head>
<body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
  <table role="presentation" style="width: 100%; border-collapse: collapse;">
    <tr>
      <td align="center" style="padding: 40px 0;">
        <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">

          <!-- Header con gradiente verde -->
          <tr>
            <td style="background: linear-gradient(135deg, #4CAF50 0%, #2E7D32 100%); padding: 40px; text-align: center; border-radius: 12px 12px 0 0;">
              <h1 style="color: #ffffff; margin: 0; font-size: 32px; font-weight: bold;">🎉 ¡Bienvenido a PRO!</h1>
            </td>
          </tr>

          <!-- Contenido principal -->
          <tr>
            <td style="padding: 40px;">
              <p style="font-size: 18px; color: #333333; margin: 0 0 20px 0;">
                Hola <strong>${userName}</strong>,
              </p>

              <p style="font-size: 16px; color: #666666; line-height: 1.6; margin: 0 0 30px 0;">
                ¡Felicidades! Tu suscripción a <strong style="color: #4CAF50;">EcoMap PRO</strong> ha sido activada exitosamente.
              </p>

              <!-- Detalles de la suscripción -->
              <table role="presentation" style="width: 100%; border-collapse: collapse; background-color: #f9f9f9; border-radius: 8px; margin-bottom: 30px;">
                <tr>
                  <td style="padding: 20px;">
                    <table role="presentation" style="width: 100%;">
                      <tr>
                        <td style="padding: 8px 0; font-size: 14px; color: #666666;">
                          <strong>Plan:</strong>
                        </td>
                        <td style="padding: 8px 0; font-size: 14px; color: #333333; text-align: right;">
                          ${planName}
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; font-size: 14px; color: #666666;">
                          <strong>Fecha de activación:</strong>
                        </td>
                        <td style="padding: 8px 0; font-size: 14px; color: #333333; text-align: right;">
                          ${activationDate}
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 8px 0; font-size: 14px; color: #666666;">
                          <strong>Email:</strong>
                        </td>
                        <td style="padding: 8px 0; font-size: 14px; color: #333333; text-align: right;">
                          ${email}
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>

              <!-- Beneficios PRO -->
              <div style="background-color: #E8F5E9; border-left: 4px solid #4CAF50; padding: 20px; border-radius: 4px; margin-bottom: 30px;">
                <h2 style="margin: 0 0 15px 0; font-size: 18px; color: #2E7D32;">
                  ✨ Tus beneficios PRO incluyen:
                </h2>
                <ul style="margin: 0; padding-left: 20px; color: #333333; font-size: 14px; line-height: 1.8;">
                  <li>Búsqueda ilimitada de productos</li>
                  <li>Acceso a todos los negocios de la comunidad</li>
                  <li>Sin anuncios publicitarios</li>
                  <li>Soporte prioritario</li>
                  <li>Acceso anticipado a nuevas funciones</li>
                </ul>
              </div>

              <!-- Call to action -->
              <div style="text-align: center; margin: 30px 0;">
                <a href="https://ecomap.app" style="display: inline-block; background-color: #4CAF50; color: #ffffff; text-decoration: none; padding: 14px 40px; border-radius: 6px; font-weight: bold; font-size: 16px;">
                  Abrir EcoMap
                </a>
              </div>

              <!-- Mensaje de agradecimiento -->
              <p style="font-size: 14px; color: #666666; line-height: 1.6; margin: 30px 0 0 0; text-align: center;">
                Gracias por confiar en EcoMap. Estamos emocionados de tenerte en nuestra comunidad PRO.
              </p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background-color: #f5f5f5; padding: 30px; text-align: center; border-radius: 0 0 12px 12px;">
              <p style="margin: 0; font-size: 12px; color: #999999; line-height: 1.6;">
                Este es un email automático, por favor no respondas a este mensaje.
              </p>
              <p style="margin: 10px 0 0 0; font-size: 12px; color: #999999;">
                © ${new Date().getFullYear()} EcoMap. Todos los derechos reservados.
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
        `
      })
    })

    const data = await res.json()

    if (res.ok) {
      return new Response(
        JSON.stringify({
          success: true,
          messageId: data.id,
          message: 'Email enviado exitosamente'
        }),
        {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
          }
        }
      )
    } else {
      console.error('Error de Resend:', data)
      return new Response(
        JSON.stringify({ error: 'Error al enviar email', details: data }),
        {
          status: 500,
          headers: {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
          }
        }
      )
    }

  } catch (error) {
    console.error('Error:', error)
    return new Response(
      JSON.stringify({ error: error.message }),
      {
        status: 500,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*'
        }
      }
    )
  }
})
