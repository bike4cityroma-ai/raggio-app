package org.bike4city.ciclofficinabot.presentation

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.bike4city.ciclofficinabot.core.designsystem.AttentionYellow
import org.bike4city.ciclofficinabot.core.designsystem.BikeGreen
import org.bike4city.ciclofficinabot.R
import org.bike4city.ciclofficinabot.domain.model.ChatRole
import org.bike4city.ciclofficinabot.domain.model.BikeProfile
import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome
import org.bike4city.ciclofficinabot.domain.model.SafetyLevel
import org.bike4city.ciclofficinabot.presentation.chat.ChatViewModel
import org.bike4city.ciclofficinabot.presentation.chat.shouldShowWorkshopContact
import org.bike4city.ciclofficinabot.presentation.history.HistoryViewModel
import org.bike4city.ciclofficinabot.presentation.history.categoryLabel
import org.bike4city.ciclofficinabot.presentation.history.outcomeLabel
import org.bike4city.ciclofficinabot.presentation.history.safetyLabel
import org.bike4city.ciclofficinabot.presentation.history.statusLabel
import org.bike4city.ciclofficinabot.presentation.report.ReportViewModel
import org.bike4city.ciclofficinabot.presentation.report.ReportSyncUiState
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncStatus
import org.bike4city.ciclofficinabot.data.local.ChatSessionEntity
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page(title: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }, navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val deepGreen = Color(0xFF071F13)
    val brightGreen = Color(0xFF74B72E)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(deepGreen)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.raggio_logo),
            contentDescription = "Raggiò, il ciclomeccanico virtuale",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = 330.dp)
        )
        Text(
            "RAGGIÒ",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            "L’assistente della ciclofficina",
            color = brightGreen,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            color = Color(0xFFF8F6ED),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Ciao! Sono Raggiò 🚲\nDimmi qual è il problema con la tua bici e ti aiuto subito!",
                modifier = Modifier.padding(24.dp),
                color = Color(0xFF102018),
                fontSize = 21.sp,
                lineHeight = 31.sp
            )
        }
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = brightGreen, contentColor = Color.White),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 66.dp)
        ) {
            Icon(Icons.Default.Build, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("Inizia a chattare", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WelcomeFeature("●●●", "Consigli\nmirati", brightGreen)
            WelcomeFeature("🔧", "Istruzioni\npasso-passo", brightGreen)
            WelcomeFeature("🚲", "Diagnosi\nproblemi", brightGreen)
            WelcomeFeature("💡", "Soluzioni\npratiche", brightGreen)
        }
    }
}

@Composable
private fun WelcomeFeature(symbol: String, label: String, color: Color) {
    Column(
        modifier = Modifier.width(78.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(symbol, color = color, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(
            label,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OnboardingScreen(onAccept: () -> Unit, onPrivacy: () -> Unit = {}) {
    var accepted by remember { mutableStateOf(false) }
    Page("Benvenuto in RAGGIÒ") {
        BrandHeader()
        Text("Un primo aiuto per la tua bici", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Descrivi il problema: riceverai domande semplici e un controllo alla volta.")
        Text("Non è una diagnosi certa", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("L'assistente non sostituisce il controllo diretto di un meccanico.")
        SafetyCard("In presenza di problemi a freni, ruote, sterzo, telaio, forcella o batteria, interrompi l'uso della bicicletta.")
        TextButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) { Text("Leggi l'informativa privacy") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(accepted, { accepted = it }, modifier = Modifier.testTag("safety-consent"))
            Text("Ho letto e accetto l'informativa di sicurezza", modifier = Modifier.padding(start = 8.dp))
        }
        Button(onClick = onAccept, enabled = accepted, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Inizia") }
        Text("Le foto vengono inviate soltanto dopo una conferma esplicita e sono eliminate automaticamente entro 7 giorni.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun HomeScreen(bikeProfile: BikeProfile, onStart: () -> Unit, onHistory: () -> Unit, onPrivacy: () -> Unit = {}) = Page("RAGGIÒ") {
    BrandHeader()
    Text("Come possiamo aiutarti?", fontSize = 26.sp, fontWeight = FontWeight.Bold)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(bikeProfile.name.ifBlank { "La mia bici" }, fontWeight = FontWeight.Bold)
            Text(buildString {
                append(bikeProfile.bikeType)
                if (bikeProfile.isElectric) append(" · e-bike")
                if (bikeProfile.brand.isNotBlank()) append(" · ${bikeProfile.brand}")
            })
        }
    }
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)) { Icon(Icons.Default.Build, null); Spacer(Modifier.width(10.dp)); Text("Descrivi un problema") }
    HomeAction("Controllo rapido della bici", "Freni, ruote, sterzo e pneumatici", onStart)
    HomeAction("Problemi frequenti", "Gomme, catena, freni, cambio ed e-bike", onStart)
    OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Build, null); Spacer(Modifier.width(8.dp)); Text("Le mie conversazioni") }
    HorizontalDivider()
    Text("RAGGIÒ · assistente online", fontWeight = FontWeight.Bold, color = BikeGreen)
    Text("Le conversazioni e i riepiloghi restano sul dispositivo. Dal riepilogo puoi scegliere se salvare anche una copia online.")
    ContactCard()
    TextButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) { Text("Informativa privacy") }
    LegalFooter()
}

@Composable
private fun HomeAction(title: String, subtitle: String, click: () -> Unit) {
    ElevatedCard(onClick = click, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle) } }
}

@Composable
fun BikeProfileScreen(initialProfile: BikeProfile, onBack: () -> Unit, onContinue: (BikeProfile) -> Unit) {
    var type by remember(initialProfile) { mutableStateOf(initialProfile.bikeType) }
    var electric by remember(initialProfile) { mutableStateOf(initialProfile.isElectric) }
    var name by remember(initialProfile) { mutableStateOf(initialProfile.name) }
    var brand by remember(initialProfile) { mutableStateOf(initialProfile.brand) }
    var model by remember(initialProfile) { mutableStateOf(initialProfile.model) }
    Page("La tua bicicletta", onBack) {
        Text("Ci serve solo qualche informazione generale. Gli altri dettagli sono facoltativi.")
        Text("Tipologia *", fontWeight = FontWeight.Bold)
        listOf("City bike", "Mountain bike", "Bici da corsa", "Cargo bike", "Altro").forEach { value ->
            FilterChip(selected = type == value, onClick = { type = value }, label = { Text(value) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(electric, { electric = it }); Text("È una e-bike", Modifier.padding(start = 12.dp)) }
        OutlinedTextField(name, { name = it }, label = { Text("Nome della bici (facoltativo)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(brand, { brand = it }, label = { Text("Marca (facoltativa)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(model, { model = it }, label = { Text("Modello (facoltativo)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(
            onClick = {
                onContinue(
                    initialProfile.copy(
                        name = name.trim().ifBlank { "La mia bici" },
                        bikeType = type,
                        isElectric = electric,
                        brand = brand.trim(),
                        model = model.trim()
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Salva e continua") }
    }
}

@Composable
fun ChatScreen(onBack: () -> Unit, onStop: (String) -> Unit, onReport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    var pendingPhoto by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current.applicationContext
    val uriHandler = LocalUriHandler.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingPhoto = uri
    }
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(context))
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.safetyLevel) {
        if (state.safetyLevel == SafetyLevel.STOP && state.sessionId.isNotBlank()) onStop(state.sessionId)
    }
    pendingPhoto?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingPhoto = null },
            title = { Text("Usare questa foto?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PhotoPreview(uri)
                    Text("L'app rimuove i metadati, ridimensiona la foto e la invia a Firebase e OpenAI per questa diagnosi.")
                    Text("La copia remota viene eliminata entro 7 giorni, oppure subito eliminando la conversazione.")
                }
            },
            confirmButton = { Button(onClick = { selectedPhoto = uri; pendingPhoto = null }) { Text("Accetto e allego") } },
            dismissButton = { TextButton(onClick = { pendingPhoto = null }) { Text("Annulla") } }
        )
    }
    Page("Diagnosi guidata", onBack) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null)
                Text("Sicurezza: ${state.safetyLevel.toItalian()}", Modifier.padding(start = 8.dp))
            }
        }
        state.messages.forEach { Message(it.role == ChatRole.USER, it.text) }
        state.instructionTitle?.let { title ->
            InstructionCard(title, state.instructionBody, state.instructionWarnings)
        }
        if (state.isLoading) Row(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Text("Sto preparando il prossimo controllo…", Modifier.padding(start = 12.dp)) }
        state.photoError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.serviceUnavailable) SafetyCard("Il servizio online non ha risposto dopo due tentativi. Attendi qualche secondo e invia nuovamente il messaggio.")
        FlowReplies(state.quickReplies) { viewModel.send(it) }
        OutlinedTextField(text, { text = it.take(1_000) }, label = { Text("Descrivi il problema") }, supportingText = { Text("${text.length}/1000") }, modifier = Modifier.fillMaxWidth())
        selectedPhoto?.let { uri ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoPreview(uri)
                    Text("Foto pronta · cancellazione automatica entro 7 giorni", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { selectedPhoto = null }) { Icon(Icons.Default.Delete, null); Text("Rimuovi") }
                }
            }
        }
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Aggiungi una foto") }
        Button(onClick = { viewModel.send(text, selectedPhoto); text = ""; selectedPhoto = null }, enabled = text.isNotBlank() && !state.isLoading, modifier = Modifier.fillMaxWidth()) { Text("Invia") }
        TextButton(onClick = { selectedPhoto = null; pendingPhoto = null; text = ""; viewModel.reset() }, modifier = Modifier.fillMaxWidth()) { Text("Nuova diagnosi") }
        if (shouldShowWorkshopContact(state.completed, state.outcome)) {
            WorkshopRecommendation { uriHandler.openUri(WORKSHOP_WHATSAPP_URL) }
        }
        if (state.completed) Button(onClick = { onReport(state.sessionId) }, modifier = Modifier.fillMaxWidth()) { Text("Vai al riepilogo") }
        OutlinedButton(onClick = viewModel::stop, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("Interrompi la procedura") }
    }
}

@Composable
private fun PhotoPreview(uri: Uri) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        runCatching { context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) } }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), "Anteprima della foto selezionata", Modifier.fillMaxWidth().heightIn(max = 220.dp))
    } else Text("Anteprima non disponibile")
}

@Composable private fun Message(user: Boolean, text: String) { Surface(color = if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(if (user) .86f else 1f).semantics { contentDescription = if (user) "Tu: $text" else "Assistente: $text" }) { Text(text, Modifier.padding(16.dp)) } }

@Composable private fun FlowReplies(values: List<String>, onClick: (String) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { OutlinedButton(onClick = { onClick(it) }, modifier = Modifier.fillMaxWidth()) { Text(it) } } } }

@Composable private fun InstructionCard(title: String, body: String?, warnings: List<String>) {
    ElevatedCard { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        body?.takeIf(String::isNotBlank)?.let { Text(it) }
        warnings.forEach { SafetyCard(it) }
    } }
}

@Composable fun StopScreen(onReport: () -> Unit) = Page("STOP — rischio rilevato") {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(Icons.Default.Warning, "Pericolo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
        Text("Non utilizzare la bicicletta", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("È stato rilevato un possibile rischio per la sicurezza. Non continuare a usare la bicicletta e non tentare riparazioni.")
        Text("Rivolgiti a personale qualificato. Evita di azionare o maneggiare inutilmente il componente interessato.")
    } }
    Button(onClick = onReport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Build, null); Spacer(Modifier.width(8.dp)); Text("Genera riepilogo") }
    ContactCard(recommendation = true)
}

@Composable
private fun BrandHeader() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_incontropedale_transparent),
                    contentDescription = "Logo Ciclofficina InControPedale",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.weight(1f).height(94.dp)
                )
                Image(
                    painter = painterResource(R.drawable.logo_bike4city_transparent),
                    contentDescription = "Logo Bike4City Roma",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.weight(1f).height(94.dp)
                )
            }
            Text("Ciclofficina InControPedale e Bike4City", fontWeight = FontWeight.Bold)
        }
    }
}

private const val WORKSHOP_WHATSAPP_URL = "https://wa.me/393516849832?text=Ciao%2C%20ho%20appena%20completato%20una%20diagnosi%20con%20Raggi%C3%B2%20e%20vorrei%20far%20controllare%20la%20mia%20bici."

@Composable
private fun WorkshopRecommendation(onWhatsApp: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ti consigliamo di passare in ciclofficina", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Scrivici subito su WhatsApp per concordare un controllo della bici.")
            Button(onClick = onWhatsApp, modifier = Modifier.fillMaxWidth()) { Text("Contatta su WhatsApp") }
        }
    }
}

@Composable
private fun ContactCard(recommendation: Boolean = false) {
    val uriHandler = LocalUriHandler.current
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (recommendation) Text("Ti consigliamo di passare in ciclofficina", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Ciclofficina InControPedale e Bike4City", fontWeight = FontWeight.Bold)
            Text("Via di Casal Bruciato 11 · 00159 Roma")
            Text("Apertura: giovedì 16:00–19:30 · sabato 10:30–13:30")
            OutlinedButton(
                onClick = { uriHandler.openUri(WORKSHOP_WHATSAPP_URL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("WhatsApp")
            }
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) = Page("Informativa privacy", onBack) {
    Text("Ultimo aggiornamento: 4 agosto 2026", style = MaterialTheme.typography.bodySmall)
    PrivacySection(
        "1. Titolare del trattamento",
        "Ciclofficina InControPedale e Bike4City, Via di Casal Bruciato 11, 00159 Roma. Per richieste relative ai dati personali: WhatsApp 351 684 9832."
    )
    PrivacySection(
        "2. Dati trattati",
        "L'app tratta il profilo generale della bicicletta, i messaggi della diagnosi, i riepiloghi e identificatori tecnici pseudonimi necessari al funzionamento e alla sicurezza. Le foto vengono trattate soltanto se selezionate e confermate dall'utente. Non sono richiesti nome, posizione, rubrica o dati sanitari."
    )
    PrivacySection(
        "3. Finalità e basi giuridiche",
        "I dati sono utilizzati per fornire la diagnosi guidata richiesta dall'utente e mantenere la cronologia sul dispositivo. Le foto e il salvataggio facoltativo del riepilogo online avvengono dopo una scelta esplicita. I controlli tecnici e antifrode sono svolti per proteggere il servizio e gli utenti."
    )
    PrivacySection(
        "4. Servizi utilizzati",
        "Firebase di Google gestisce autenticazione anonima, funzioni cloud, protezione dell'app e, quando richiesto, foto e copie online dei riepiloghi. OpenAI elabora messaggi e sole foto confermate per produrre la risposta. Le richieste OpenAI usano store:false e i contenuti API non sono usati per addestrare i modelli salvo adesione esplicita del titolare; OpenAI può conservare log di sicurezza fino a 30 giorni, salvo obblighi di legge o condizioni particolari di sicurezza."
    )
    PrivacySection(
        "5. Conservazione",
        "Conversazioni e riepiloghi locali restano sul telefono finché l'utente li elimina o disinstalla l'app. Le foto remote sono eliminate automaticamente entro 7 giorni e, quando tecnicamente possibile, subito dopo la richiesta di eliminazione della conversazione. Le copie online facoltative dei riepiloghi restano fino alla cancellazione richiesta dall'utente."
    )
    PrivacySection(
        "6. Comunicazione e trasferimenti",
        "I dati non sono venduti e non sono usati per pubblicità. Google e OpenAI agiscono come fornitori tecnologici e possono trattare dati anche fuori dallo Spazio economico europeo applicando le garanzie previste dai rispettivi accordi e dalla normativa applicabile. Un contatto avviato tramite WhatsApp è inoltre soggetto alle condizioni e all'informativa di WhatsApp."
    )
    PrivacySection(
        "7. Diritti dell'interessato",
        "L'utente può chiedere accesso, rettifica, cancellazione, limitazione, opposizione e portabilità quando applicabili, oltre a revocare un consenso senza pregiudicare i trattamenti già effettuati. Può eliminare conversazioni e copie online dall'app o contattare il titolare. È possibile presentare reclamo al Garante per la protezione dei dati personali."
    )
    PrivacySection(
        "8. Sicurezza e limiti del servizio",
        "I trasferimenti avvengono tramite connessioni cifrate; backup e trasferimento automatico dei dati diagnostici sono disabilitati. L'assistente fornisce un primo orientamento prudente e non sostituisce il controllo di un meccanico qualificato."
    )
    LegalFooter()
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(body)
    }
}

@Composable
private fun LegalFooter() {
    HorizontalDivider(Modifier.padding(top = 8.dp))
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "© 2026 Ciclofficina InControPedale e Bike4City. Tutti i diritti riservati.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Text(
            "Raggiò offre un primo orientamento e non sostituisce il controllo di un meccanico qualificato.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Text(
            "Servizio realizzato con tecnologie Firebase di Google e OpenAI. I relativi nomi e marchi appartengono ai rispettivi titolari.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable fun ReportScreen(sessionId: String, onHome: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val viewModel: ReportViewModel = viewModel(factory = ReportViewModel.factory(context, sessionId))
    val report by viewModel.report.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    var confirmUpload by remember { mutableStateOf(false) }
    var confirmRemoteDelete by remember { mutableStateOf(false) }

    if (confirmUpload) {
        AlertDialog(
            onDismissRequest = { confirmUpload = false },
            title = { Text("Salvare una copia online?") },
            text = {
                Text(
                    "Verranno inviati a Firebase il riepilogo della bici, il problema descritto, " +
                        "le verifiche e l'esito. Non vengono inviati foto, posizione o contatti. " +
                        "L'accesso è associato a un identificatore anonimo."
                )
            },
            confirmButton = {
                Button(onClick = { confirmUpload = false; viewModel.syncReport() }) {
                    Text("Invia report")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUpload = false }) { Text("Annulla") }
            }
        )
    }

    if (confirmRemoteDelete) {
        AlertDialog(
            onDismissRequest = { confirmRemoteDelete = false },
            title = { Text("Eliminare la copia online?") },
            text = { Text("Il report resterà disponibile su questo dispositivo, ma verrà rimosso da Firebase.") },
            confirmButton = {
                Button(onClick = { confirmRemoteDelete = false; viewModel.removeRemoteCopy() }) {
                    Text("Elimina online")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoteDelete = false }) { Text("Annulla") }
            }
        )
    }

    Page("Riepilogo per la ciclofficina") {
        val value = report
        if (value == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            Text("Preparazione del riepilogo…", modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            val isRed = value.outcome == DiagnosisOutcome.RED
            Text(
                if (isRed) "Esito: ROSSO — non usare la bici" else "Esito: GIALLO — verifica consigliata",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRed) MaterialTheme.colorScheme.error else Color(0xFF7A5700)
            )
            ReportRow("Bicicletta", value.bikeSummary)
            ReportRow("Problema", value.initialProblem)
            ReportRow("Verifiche", value.checksPerformed.joinToString("; "))
            ReportRow("Possibili cause", value.possibleCauses.joinToString("; "))
            ReportRow("Uso della bici", value.usageRecommendation)
            Text("Salvato sul dispositivo", style = MaterialTheme.typography.bodySmall)
            FirebaseSyncCard(
                state = syncState,
                onUpload = { confirmUpload = true },
                onDeleteRemote = { confirmRemoteDelete = true }
            )
            if (value.outcome == DiagnosisOutcome.YELLOW || value.outcome == DiagnosisOutcome.RED) {
                ContactCard(recommendation = true)
            }
        }
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Torna alla home") }
    }
}

@Composable
private fun FirebaseSyncCard(
    state: ReportSyncUiState,
    onUpload: () -> Unit,
    onDeleteRemote: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Copia online facoltativa", fontWeight = FontWeight.Bold)
            when (state) {
                ReportSyncUiState.Unavailable ->
                    Text("Firebase non è disponibile in questa build. Il report resta sul dispositivo.")
                ReportSyncUiState.LocalOnly -> {
                    Text("Il riepilogo non è stato salvato online e resta soltanto su questo dispositivo.")
                    OutlinedButton(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                        Text("Salva anche online")
                    }
                }
                ReportSyncUiState.Syncing ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp))
                        Text("Invio protetto in corso…", Modifier.padding(start = 10.dp))
                    }
                ReportSyncUiState.Removing ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp))
                        Text("Eliminazione della copia online…", Modifier.padding(start = 10.dp))
                    }
                is ReportSyncUiState.Synced -> {
                    val date = if (state.syncedAt > 0L) {
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(state.syncedAt))
                    } else "data non disponibile"
                    Text("Copia online aggiornata: $date", color = BikeGreen, fontWeight = FontWeight.Bold)
                    Text("Il report conservato su questo telefono resta la copia principale.")
                    TextButton(onClick = onDeleteRemote, modifier = Modifier.fillMaxWidth()) {
                        Text("Elimina solo la copia online")
                    }
                }
                is ReportSyncUiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                        Text("Riprova")
                    }
                }
            }
        }
    }
}

@Composable private fun ReportRow(label: String, value: String) { Column { Text(label, fontWeight = FontWeight.Bold); Text(value); HorizontalDivider(Modifier.padding(top = 12.dp)) } }

@Composable fun HistoryScreen(onBack: () -> Unit, onReport: (String) -> Unit) {
    val context = LocalContext.current.applicationContext
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(context))
    val sessions by viewModel.sessions.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    var pendingDelete by remember { mutableStateOf<ChatSessionEntity?>(null) }

    pendingDelete?.let { session ->
        val online = session.remoteSyncStatus == ReportSyncStatus.SYNCED.name
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminare la conversazione?") },
            text = {
                Text(
                    if (online) {
                        "La conversazione verrà eliminata dal dispositivo e la sua copia verrà rimossa da Firebase."
                    } else {
                        "La conversazione e il riepilogo verranno eliminati da questo dispositivo."
                    }
                )
            },
            confirmButton = {
                Button(onClick = { pendingDelete = null; viewModel.delete(session) }) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annulla") }
            }
        )
    }

    Page("Le mie conversazioni", onBack) {
        deleteState.error?.let { error ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = viewModel::dismissError) { Text("Chiudi") }
                }
            }
        }
        if (sessions.isEmpty()) {
            Icon(Icons.Default.Build, null, Modifier.size(64.dp).align(Alignment.CenterHorizontally))
            Text("Nessuna conversazione salvata", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Avvia una diagnosi: i dati restano sul dispositivo finché non scegli di inviare un riepilogo.")
        } else sessions.forEach { session ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(session.title, fontWeight = FontWeight.Bold)
                    Text("${session.bikeName} · ${categoryLabel(session.category)}")
                    Text("${statusLabel(session.status)} · rischio ${safetyLabel(session.safetyLevel)} · esito ${outcomeLabel(session.outcome)}")
                    Text(
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(session.updatedAt)),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (session.status == "COMPLETED") {
                        Button(onClick = { onReport(session.id) }) { Text("Apri riepilogo") }
                    }
                    when (session.remoteSyncStatus) {
                        ReportSyncStatus.SYNCED.name -> Text("Copia online presente", color = BikeGreen)
                        ReportSyncStatus.FAILED.name ->
                            Text("Sincronizzazione da riprovare", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(
                        onClick = { pendingDelete = session },
                        enabled = deleteState.deletingSessionId == null
                    ) {
                        Text(
                            if (deleteState.deletingSessionId == session.id) {
                                "Eliminazione…"
                            } else {
                                "Elimina conversazione"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun SafetyCard(text: String) { Surface(color = AttentionYellow.copy(alpha = .20f), shape = RoundedCornerShape(12.dp), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) { Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Default.Warning, "Attenzione"); Text(text, Modifier.weight(1f)) } } }

private fun SafetyLevel.toItalian() = when (this) {
    SafetyLevel.SAFE -> "normale"
    SafetyLevel.CAUTION -> "attenzione"
    SafetyLevel.STOP -> "stop"
}
