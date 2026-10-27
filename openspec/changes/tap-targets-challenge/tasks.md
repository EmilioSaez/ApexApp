## 0. Piezas compartidas (commit `123561f`)

- [x] 0.1 `TouchPlausibility(level)` + test
- [x] 0.2 Plumbing del nivel efectivo (`LocalTouchGuard`, `RingActivity` → `ChallengeSessionScreen`)
- [x] 0.3 `SuspicionReason.SYNTHETIC_INPUT` (3 descartes en `STRICT`) + test
- [x] 0.4 `ChallengeAnswer.GameVerified`

## 1. Lógica de dominio (`domain/challenge/touch/`)

- [ ] 1.1 `TapTargetsParams`: lectura tolerante de `{"rounds":N}` (defecto 8, rango 3..15) y test
- [ ] 1.2 `TapTargetsTuning.BY_LEVEL` (círculos, diámetro y plazo por nivel 1–5) y test de que el nivel 5 es más exigente que el 1
- [ ] 1.3 Generador de rondas (círculos del nivel sin solape, coordenadas 0..1, `Random` de la sesión) y test
- [ ] 1.4 Regla de rondas y penalización (fallo → +1 extra con tope, sin reinicio) y test
- [ ] 1.6 `TapTargetsChallenge`/instancia, `GameVerified` y test de `check`

## 2. UI

- [ ] 2.1 `TapTargetsRenderer`: zona de juego, círculos, temporizador de ronda, progreso, semántica de accesibilidad
- [ ] 2.2 `TapTargetsParamsEditor`: stepper de rondas
- [ ] 2.3 Usar `rememberTouchPlausibility()` en el renderer con los `MotionEvent` (origen, ventana tapada, instante)
- [ ] 2.4 Strings es/en (bloque propio) y etiquetas en `ChallengeLabels`/`ImportScreen`

## 3. Registro e integración

- [ ] 3.1 `@Binds @IntoSet` de reto, renderer y editor en `ChallengeModule`
- [ ] 3.2 Comprobar editor (modo Normal / Con retos), pantalla de prueba y sonado en emulador

## 4. Cierre

- [ ] 4.1 Ejecutar los tests en este worktree y revisar el diff (secretos, ficheros sobrantes)
- [ ] 4.2 Actualizar README/docs de retos si lista los tipos
