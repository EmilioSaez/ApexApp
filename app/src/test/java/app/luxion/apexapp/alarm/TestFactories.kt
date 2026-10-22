package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.FireStateRepository
import app.luxion.apexapp.domain.WakeEventRecorder
import java.time.Clock

object NoOpNotifier : AlarmNotifier {
    override fun notify(alarm: Alarm, sessionId: String) = Unit
    override fun cancel(alarmId: Long) = Unit
}

class CountingSessionIds : SessionIdGenerator {
    private var counter = 0
    override fun next() = "session-${++counter}"
}

/** Controlador con todas sus dependencias falsas, para tests de ViewModel y de reprogramación. */
fun testController(
    repository: FakeAlarmRepository,
    gateway: FakeGateway,
    clock: Clock,
    launcher: RingingLauncher = FakeRingingLauncher(),
    fireState: FireStateRepository = FakeFireStateRepository(),
    recorder: WakeEventRecorder = FakeWakeEventRecorder(),
): AlarmController {
    val scheduler = AlarmClockScheduler(gateway, clock, fireState)
    val handler = AlarmFiringHandler(
        repository, scheduler, RingingRegistry(), launcher, NoOpNotifier, CountingSessionIds(), fireState, clock,
        FakeWakeEventRecorder(),
    )
    return AlarmController(repository, scheduler, fireState, handler, recorder, clock)
}
