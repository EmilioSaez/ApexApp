# Design

## Context

Los retos son `Challenge` (fábrica) + `ChallengeInstance` (`check(answer)`), registrados con `@Binds @IntoSet` en `ChallengeModule`, con un `ChallengeRenderer` y, opcionalmente, un `ChallengeParamsEditor` por tipo. `ChallengeSession` regenera la instancia ante `check == false` y registra `ChallengeFailed`. La dificultad (1..5) es global por alarma; Mates y Pasos derivan su dificultad solo del nivel y **no** tienen parámetro de rondas: QR es el único tipo que usa `ChallengeSpec.params` (JSON) con editor propio. Los niveles anti-trampas no afectan a los retos: gobiernan lockdown y marcado de sesión sospechosa.

## Goals / Non-Goals

**Goals:**
- Lógica de rondas pura, determinista con `Random` y reloj inyectados, testeable en JVM.
- Cambio mínimo en los puntos compartidos (registro, `ChallengeAnswer`, etiquetas, strings).

**Non-Goals:**
- Nuevo nivel anti-trampas o detección propia de toques sintéticos.
- Cambios en `ChallengeSession`, el modo Normal / Con retos o la dificultad adaptativa.
- Los retos «Tocar objetivos» y «Ritmo» (otros changes).

## Decisions

1. **Rondas en `params` (`{"rounds":N}`).** Es el único mecanismo existente de parámetro por tipo (QR) y evita tocar `SessionConfig`. Alternativa: derivar las rondas del nivel como Pasos; descartada porque se pidió un parámetro configurable propio. Rango 2..6, defecto 3, parseo tolerante.
2. **Máquina de estados pura** en `domain/challenge/holdrelease/`: recibe `press(t)`, `release(t)`, `cancel()` y `tick(t)` y expone fase (espera, mantenimiento, señal, hecho), rondas hechas/total y resultado de cada ronda. Tiempos en ms de reloj monotónico. Alternativa: lógica en el composable; descartada por no ser testeable.
3. **Tiempos (tabla única, calibrable):** mantenimiento antes de la señal 1,5–4 s; ventana de acierto por nivel 1→5: 1200, 1000, 800, 650, 500 ms. Con 3 rondas, ~20 s por ronda incluidas pausas y fallos esperables ≈ 1 min.
4. **Fallo = penalización dentro del juego.** Cada fallo repite la ronda con nuevo sorteo y suma una ronda extra (tope +3); tres fallos seguidos reinician solo esa ronda. El progreso vive en el juego, no en la instancia: si cada fallo se enviara a `ChallengeSession` como respuesta incorrecta, esta regeneraría la instancia y se perdería el progreso. El renderer solo llama a `onAnswer` al superar todas las rondas.
5. **Respuesta común `ChallengeAnswer.GameVerified(type)`**, introducida por el commit compartido de los juegos táctiles; este change no añade variante propia.
6. **Aleatoriedad por sesión:** el `Random` llega por `generate(level, params, random)` y la instancia lo conserva para sortear la señal de cada ronda.
7. **Anti-trampas:** sin nivel propio. Lockdown y `session-integrity` cubren las salidas. Las pulsaciones se validan con `TouchPlausibility(level)` compartido (nivel de la alarma llevado hasta el renderer, `SuspicionReason.SYNTHETIC_INPUT`, marca de sospechosa en STRICT), todo del commit compartido. Este reto solo aporta lo específico de pulsar y soltar: superposición de ventana, origen del evento y tiempos de reacción (soltar − señal) sintéticamente idénticos entre rondas, ya que la duración de la pulsación varía por el sorteo de la señal. La pérdida de foco o la cancelación cuenta como fallo de ronda. No hay cuenta atrás visible.
8. **UI:** control grande central; la señal cambia forma/texto además del color (accesibilidad). Editor de rondas con selector 2..6 y duración estimada.
9. **Id de tipo `holdrelease`**; etiquetas `challenge_type_holdrelease` en `values` y `values-en`.

## Risks / Trade-offs

- Los fallos intermedios no llegan al historial de despertar (solo el resultado final) → aceptable ahora; ampliable si se quiere.
- Un toque automatizado (accesibilidad/ADB) podría superar el reto → mitigado por el instante aleatorio y la ventana estrecha; el resto lo cubre el lockdown de la alarma.
- Dependencia del commit compartido: no se puede conectar el renderer hasta integrarlo (merge o cherry-pick) → se avanza antes con lógica pura, tabla de tiempos y parámetros.
- Conflictos de merge con los otros dos juegos en `ChallengeModule`, etiquetas y strings → añadidos mínimos en bloque propio.
- Ventanas sin calibrar con usuarios → tabla única ajustable.
