package tn.smartrecruit.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import tn.smartrecruit.data.model.Role
import tn.smartrecruit.data.remote.dto.RegisterCandidateRequest
import tn.smartrecruit.data.remote.dto.RegisterRecruiterRequest
import tn.smartrecruit.di.appViewModelFactory
import tn.smartrecruit.ui.components.AppTextField
import tn.smartrecruit.ui.components.AuthScaffold
import tn.smartrecruit.ui.components.ErrorText
import tn.smartrecruit.ui.components.PrimaryButton
import tn.smartrecruit.ui.theme.TextMuted

// ---------------- Splash ----------------
@Composable
fun SplashScreen(onFinished: (Role) -> Unit) {
    val vm: SessionViewModel = viewModel(factory = appViewModelFactory())
    val role by vm.role.collectAsStateWithLifecycle()

    LaunchedEffect(role) {
        val current = role
        if (current != null) {
            delay(1200)
            onFinished(current)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // TODO: replace with the real logo (res/drawable) once exported from Figma
            Text("Smart Recruit AI", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("Find . Match . Grow", color = TextMuted)
        }
    }
}

// ---------------- Welcome ----------------
@Composable
fun WelcomeScreen(onCreateAccount: () -> Unit, onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to SmartRecruit AI", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Find the right match between your skills and the best job offers.", color = TextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Créer un compte", onCreateAccount)
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Se connecter à un compte existent", onLogin)
    }
}

// ---------------- Choose role ----------------
@Composable
fun ChooseRoleScreen(onContinue: (String) -> Unit, onBack: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf("candidate") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("A better match starts here", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("How would you like to use SmartRecruit AI?", color = TextMuted)

        RoleCard(
            icon = Icons.Filled.Person,
            title = "I'm a Candidate",
            description = "Discover roles that fit your skills and take your next career step.",
            selected = selected == "candidate",
            onClick = { selected = "candidate" }
        )
        RoleCard(
            icon = Icons.Filled.Business,
            title = "I'm a Recruiter",
            description = "Find the right people. Your account must be approved by an administrator.",
            selected = selected == "recruiter",
            onClick = { selected = "recruiter" }
        )

        Spacer(Modifier.weight(1f))
        PrimaryButton("Continue", { onContinue(selected) })
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Retour") }
    }
}

@Composable
private fun RoleCard(icon: ImageVector, title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else TextMuted.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, color = TextMuted, fontSize = 13.sp)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

// ---------------- Login ----------------
@Composable
fun LoginScreen(onLoggedIn: (Role) -> Unit, onCreateAccount: () -> Unit) {
    val vm: LoginViewModel = viewModel(factory = appViewModelFactory())
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state) {
        (state as? LoginUiState.Success)?.let { onLoggedIn(it.role) }
    }

    AuthScaffold(title = "Se connecter") {
        AppTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        AppTextField(password, { password = it }, "Mot de passe", isPassword = true)
        ErrorText((state as? LoginUiState.Error)?.message)
        PrimaryButton(
            text = "Se connecter",
            onClick = { vm.login(email, password) },
            loading = state is LoginUiState.Loading
        )
        TextButton(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) {
            Text("Pas de compte ? Créer un compte")
        }
    }
}

// ---------------- Register (candidate / recruiter) ----------------
@Composable
fun RegisterScreen(
    role: String,
    onCandidateRegistered: () -> Unit,
    onRecruiterRequested: () -> Unit,
    onLogin: () -> Unit
) {
    val vm: RegisterViewModel = viewModel(factory = appViewModelFactory())
    val state by vm.state.collectAsStateWithLifecycle()
    val isRecruiter = role == "recruiter"

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    // candidate only
    var phone by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    // recruiter only
    var company by rememberSaveable { mutableStateOf("") }
    var position by rememberSaveable { mutableStateOf("") }
    var website by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is RegisterUiState.Success) {
            if (isRecruiter) onRecruiterRequested() else onCandidateRegistered()
        }
    }

    AuthScaffold(
        title = if (isRecruiter) "Demande de compte recruteur" else "Créer un compte",
        subtitle = if (isRecruiter) "Un administrateur valide chaque demande avant l'activation du compte."
        else "Déposez votre CV en deux minutes."
    ) {
        AppTextField(fullName, { fullName = it }, "Nom complet")
        AppTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        AppTextField(password, { password = it }, "Mot de passe", isPassword = true)

        if (isRecruiter) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(company, { company = it }, "Entreprise", modifier = Modifier.weight(1f))
                AppTextField(position, { position = it }, "Votre poste", modifier = Modifier.weight(1f))
            }
            AppTextField(website, { website = it }, "Site web de l'entreprise", placeholder = "https://", keyboardType = KeyboardType.Uri)
            AppTextField(
                description, { description = it }, "Présentation de l'entreprise",
                placeholder = "Activité, taille, ce que vous recrutez…", singleLine = false, minLines = 3
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(phone, { phone = it }, "Téléphone", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Phone)
                AppTextField(city, { city = it }, "Ville", modifier = Modifier.weight(1f))
            }
        }

        ErrorText((state as? RegisterUiState.Error)?.message)

        PrimaryButton(
            text = if (isRecruiter) "Envoyer ma demande" else "Créer mon compte",
            loading = state is RegisterUiState.Loading,
            onClick = {
                if (isRecruiter) {
                    vm.registerRecruiter(RegisterRecruiterRequest(fullName.trim(), email.trim(), password, company.trim(), position.trim(), website.trim(), description.trim()))
                } else {
                    vm.registerCandidate(RegisterCandidateRequest(fullName.trim(), email.trim(), password, phone.trim(), city.trim()))
                }
            }
        )
        TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Déjà inscrit ? Se connecter") }
    }
}

// ---------------- Pending approval (recruiter) ----------------
@Composable
fun PendingApprovalScreen(onBackToLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 16.dp))
        Text("Demande envoyée", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Un administrateur doit valider votre demande avant que vous puissiez vous connecter en tant que recruteur.",
            color = TextMuted, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) { Text("Retour à la connexion") }
    }
}
