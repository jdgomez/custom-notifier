# localization Specification

## Purpose
The app is available in English and Spanish: how the language is chosen, the approved Spanish texts, and how the notification channel and calendar events follow a language change.

## Requirements

### Requirement: Supported languages
The app SHALL be available in English and Spanish. It SHALL show its texts in Spanish when the app language is Spanish (any region), and in English otherwise. The app language SHALL be the device language, or the language chosen for the app in the system settings on Android 13 and later, where the app SHALL offer English and Spanish. The app SHALL NOT have its own language setting. Dates SHALL use the medium format of the app language. "Custom Notifier" and texts entered by the user (product names, units) SHALL NOT be translated.

#### Scenario: Device in Spanish
- **GIVEN** the device language is Spanish (Mexico)
- **WHEN** the user opens the app with no products
- **THEN** the product list shows "Aún no hay productos"

#### Scenario: Other language falls back to English
- **GIVEN** the device language is French
- **WHEN** the user opens the app with no products
- **THEN** the product list shows "No products yet"

#### Scenario: Language chosen for the app
- **GIVEN** Android 13 or later, the device in English and Spanish chosen for Custom Notifier in the system settings
- **WHEN** the user opens the app
- **THEN** the app is in Spanish

#### Scenario: Dates and user data
- **GIVEN** the app in Spanish and a product "Coffee" that runs out on 2026-03-11
- **WHEN** the product list is shown
- **THEN** the row shows "Coffee" and "Se agota el 11 mar 2026"

### Requirement: Spanish texts
Every text the app shows SHALL have the Spanish version below, informal ("tú"), with the same placeholders as the English text (`%1$s` and so on, in any order the sentence needs). Texts that are only placeholders or symbols SHALL be the same in both languages.

| English | Spanish |
|---|---|
| No products yet | Aún no hay productos |
| Products you add will appear here with their stock and next alert. | Los productos que añadas aparecerán aquí con sus existencias y su próxima alerta. |
| about %1$s %2$s | aproximadamente %1$s %2$s |
| Runs out %1$s | Se agota el %1$s |
| Alert %1$s | Alerta el %1$s |
| Alert due | Alerta pendiente |
| Ran out %1$s | Se agotó el %1$s |
| Add product | Añadir producto |
| Edit | Editar |
| New product | Nuevo producto |
| Edit product | Editar producto |
| Navigate up | Volver |
| Save | Guardar |
| Name | Nombre |
| Unit | Unidad |
| What you count, as it will be shown: pills, diapers, ml | Lo que cuentas, tal como se mostrará: pastillas, pañales, ml |
| Units per package | Unidades por paquete |
| Units consumed | Unidades consumidas |
| units | unidades |
| %1$s every | %1$s cada |
| Days between consumption | Días entre consumos |
| days | días |
| Alert (before the lead time field) | Avisar |
| Days before it runs out | Días antes de que se agote |
| days before it runs out | días antes de que se agote |
| 0 alerts on the day it runs out | 0 avisa el mismo día en que se agota |
| Units you have now | Unidades que tienes ahora |
| Enter a name | Escribe un nombre |
| Enter a unit | Escribe una unidad |
| Enter a whole number from %1$s to %2$s | Escribe un número entero de %1$s a %2$s |
| Delete product | Eliminar producto |
| Delete %1$s? | ¿Eliminar %1$s? |
| Its stock and alert will be removed. This can't be undone. | Se eliminarán sus existencias y su alerta. No se puede deshacer. |
| Cancel | Cancelar |
| Delete | Eliminar |
| Discard changes? | ¿Descartar los cambios? |
| Keep editing | Seguir editando |
| Discard | Descartar |
| Stock | Existencias |
| Restock | Reponer |
| Adjust | Ajustar |
| Stock after: | Existencias después: |
| Restock %1$s | Reponer %1$s |
| Fewer packages | Menos paquetes |
| More packages | Más paquetes |
| Adjust stock | Ajustar existencias |
| Add | Añadir |
| Remove | Quitar |
| Units | Unidades |
| Depletion alerts | Alertas de agotamiento |
| Buy %1$s | Comprar %1$s |
| Runs out %1$s. Added by Custom Notifier. | Se agota el %1$s. Añadido por Custom Notifier. |
| Runs out today | Se agota hoy |
| Alerts are off. You will not be told before a product runs out. | Las alertas están desactivadas. No se te avisará antes de que se agote un producto. |
| Turn on | Activar |
| Calendar alerts are off. Calendar access was removed. | Las alertas del calendario están desactivadas. Se quitó el acceso al calendario. |
| Calendar alerts are off. The chosen calendar is no longer available. | Las alertas del calendario están desactivadas. El calendario elegido ya no está disponible. |
| Fix | Solucionar |
| Allow alerts? | ¿Permitir alertas? |
| Custom Notifier shows one notification before a product runs out, so you can buy more in time. Without it you get no alerts. | Custom Notifier muestra una notificación antes de que se agote un producto, para que puedas comprar más a tiempo. Sin ella no recibirás alertas. |
| Continue | Continuar |
| Not now | Ahora no |
| Settings | Ajustes |
| Back | Atrás |
| Calendar | Calendario |
| Add alerts to my calendar | Añadir alertas a mi calendario |
| Each alert also appears as an all-day event in the calendar you choose. | Cada alerta aparece también como un evento de día completo en el calendario que elijas. |
| The chosen calendar is no longer available. Choose another one. | El calendario elegido ya no está disponible. Elige otro. |
| Calendar access was removed. Alerts are not added to your calendar. | Se quitó el acceso al calendario. Las alertas no se añaden a tu calendario. |
| Allow | Permitir |
| Allow calendar access? | ¿Permitir el acceso al calendario? |
| Custom Notifier uses calendar access only to list your calendars and to add one all-day event for each alert. | Custom Notifier usa el acceso al calendario solo para mostrar tus calendarios y añadir un evento de día completo por cada alerta. |
| Calendar access is off | El acceso al calendario está desactivado |
| To add alerts to your calendar, allow calendar access for Custom Notifier in the system settings. | Para añadir alertas a tu calendario, permite el acceso al calendario de Custom Notifier en los ajustes del sistema. |
| Open settings | Abrir ajustes |
| Choose a calendar | Elige un calendario |
| No calendar available | No hay ningún calendario disponible |
| There is no calendar on this device that Custom Notifier can add events to. Add an account in your calendar app, then try again. | No hay ningún calendario en este dispositivo al que Custom Notifier pueda añadir eventos. Añade una cuenta en tu app de calendario y vuelve a intentarlo. |
| OK | Aceptar |

#### Scenario: Every text translated
- **GIVEN** the English texts of the app
- **WHEN** the Spanish texts are checked
- **THEN** every translatable English text has a Spanish text with the same placeholders, and there is no Spanish text without an English one

#### Scenario: Notification in Spanish
- **GIVEN** the app in Spanish and a product "Café" with a depletion date of 2026-03-11
- **WHEN** its alert is delivered
- **THEN** the notification has the title "Comprar Café" and the text "Se agota el 11 mar 2026"

### Requirement: Texts written outside the app follow the language
The name of the notification channel SHALL be the one of the current app language, updated when the app starts and when the device language changes. Each alert event SHALL be written in the app language at the time it is written. When the app language changes, the next synchronization of alert events SHALL rewrite, in the new language, every alert event written in another language; a change of the device language SHALL trigger that synchronization. An event deleted outside the app SHALL still not be recreated by a language change.

#### Scenario: Channel name after a language change
- **GIVEN** the app installed and opened with the device in English
- **WHEN** the device language is changed to Spanish
- **THEN** the app's notification channel in the system settings is named "Alertas de agotamiento"

#### Scenario: Events rewritten after a language change
- **GIVEN** the calendar channel on and the event "Buy Coffee" written in English
- **WHEN** the device language is changed to Spanish
- **THEN** the event is titled "Comprar Coffee" and described in Spanish

#### Scenario: Deleted event not recreated by a language change
- **GIVEN** the event "Buy Coffee" deleted by the user in the calendar app
- **WHEN** the device language is changed to Spanish
- **THEN** no event for "Coffee" is created
