# Tasks

## 1. Dominio

- [x] 1.1 Añadir la dependencia `image-labeling` al catálogo de versiones y a `app/build.gradle.kts`
- [x] 1.2 `ObjectPhotoChallenge` con `ChallengeType("object")`, `params` con la etiqueta y disponibilidad según cámara y permiso
- [x] 1.3 Respuesta del reto (variante de `ChallengeAnswer` con la etiqueta verificada) y comprobación en la instancia
- [x] 1.4 Adaptador de etiquetas a `FrameObservation` con umbral de confianza y reutilización de `FrameLivenessChecker`
- [x] 1.5 Tests de dominio: etiqueta coincidente, distinta, intermitente, foto estática y pantalla

## 2. UI

- [x] 2.1 Analizador de fotogramas con ML Kit conectado al componente de cámara del reto QR
- [x] 2.2 `ObjectPhotoRenderer` con progreso, avisos de rechazo y permiso denegado
- [x] 2.3 `ObjectPhotoParamsEditor` con vista previa, etiquetas detectadas, selección y bloqueo de guardado sin etiqueta
- [x] 2.4 Bindings en `ChallengeModule`, etiquetas en `ChallengeLabels` e `ImportScreen`, y strings es/en

## 3. Verificación

- [x] 3.1 Resolver la pregunta abierta de varios objetos por alarma con el usuario antes de implementar (un objeto por alarma; una foto por reto de foto)
- [ ] 3.2 `./gradlew test assembleDebug` y prueba manual en emulador/dispositivo (registro, éxito, foto de foto, sin permiso)
