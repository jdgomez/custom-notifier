# Política de privacidad - Custom Notifier

Última actualización: 2026-10-21. [English version](privacy-policy.md)

Custom Notifier es una aplicación de Android que te avisa antes de que se acabe un producto de consumo del hogar. La desarrolla una persona a título individual ("el desarrollador").

## Resumen

Todos tus datos se quedan en tu dispositivo. La aplicación no tiene cuenta, ni analítica, ni publicidad, ni servidor. El desarrollador no recibe ningún dato de la aplicación y no comparte nada con nadie.

## Qué guarda la aplicación

Los productos que introduces (nombre, unidad, tamaño del envase, consumo, antelación del aviso y existencias) se guardan solo en el almacenamiento privado de la aplicación en tu dispositivo. La copia de seguridad de Android y la transferencia entre dispositivos están desactivadas para la aplicación, así que la aplicación no copia estos datos a ninguna nube. Al desinstalar la aplicación se borran.

## Permisos y para qué se usan

- **Notificaciones (`POST_NOTIFICATIONS`).** Para mostrarte una notificación local antes de que se acabe un producto. Las notificaciones se crean en tu dispositivo; no se envía nada a ninguna parte. La aplicación pide este permiso después de que guardes un producto.
- **Lectura y escritura del calendario (`READ_CALENDAR`, `WRITE_CALENDAR`).** Solo si activas "Añadir alertas a mi calendario" en Ajustes, y solo después de que lo permitas. La aplicación lista los calendarios de tu dispositivo para que elijas uno, escribe un evento de todo el día por cada alerta (con el título "Comprar <producto>" y la fecha en que se agota) en el calendario que elegiste, y vuelve a leer sus propios eventos para mantenerlos al día. No lee tus otros eventos para ningún otro fin. Si no lo activas, la aplicación nunca toca tu calendario. Puedes desactivarlo en Ajustes o retirar el permiso en los ajustes del sistema en cualquier momento.
- **Ejecutarse tras reiniciar (`RECEIVE_BOOT_COMPLETED`).** Para programar de nuevo tus alertas cuando el dispositivo se reinicia. No envía ningún dato.

## Servicios de calendario ajenos a la aplicación

Los eventos que escribe la aplicación viven en el calendario que elegiste. Si ese calendario se sincroniza con una cuenta de tu dispositivo (por ejemplo una cuenta de Google), la aplicación de calendario y el proveedor de esa cuenta sincronizan los eventos bajo su propia política de privacidad. La aplicación no controla esa sincronización y no envía los eventos a ninguna parte por sí misma. Si no quieres que se sincronicen, elige un calendario local del dispositivo.

## Lo que la aplicación no hace

La aplicación no tiene permiso de internet. No recoge datos personales, no usa servicios de analítica ni de informes de fallos, no muestra anuncios, no tiene cuentas y no comparte datos con terceros.

## Menores

La aplicación es una herramienta doméstica general y no está dirigida a menores.

## Cambios

Si esta política cambia, la nueva versión se publicará en esta misma dirección con una nueva fecha.

## Contacto

Abre una incidencia en <https://github.com/jdgomez/custom-notifier/issues>.
