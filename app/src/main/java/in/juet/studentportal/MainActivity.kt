package `in`.juet.studentportal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

private val Ink = Color(0xFF17243B)
private val Green = Color(0xFF167D65)
private val PaleGreen = Color(0xFFE7F5EF)
private val Canvas = Color(0xFFF5F7FA)
private val Muted = Color(0xFF6B778A)
private val Orange = Color(0xFFCA7A20)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PortalTheme { PortalRoot() } }
    }
}

@Composable
private fun PortalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green, onPrimary = Color.White, secondary = Ink,
            background = Canvas, surface = Color.White, onSurface = Ink, onBackground = Ink
        ),
        content = content
    )
}

private enum class PortalTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Attendance("Attendance", Icons.Default.CheckCircle),
    Results("Results", Icons.Default.School),
    Timetable("Timetable", Icons.Default.CalendarMonth),
    Notices("Notices", Icons.Default.Notifications),
    Profile("Profile", Icons.Default.Person)
}

class PortalViewModel : ViewModel() {
    var signedIn by mutableStateOf(false)
        private set
    var selectedTab by mutableStateOf(PortalTab.Home)
        private set
    var loginError by mutableStateOf<String?>(null)
        private set
    fun demoSignIn(id: String, password: String) {
        if (id.isBlank() || password.isBlank()) {
            loginError = "Enter a demo student ID and password."
            return
        }
        // Demo-only: the password is neither transmitted nor stored.
        loginError = null
        signedIn = true
    }
    fun signOut() { signedIn = false; selectedTab = PortalTab.Home }
    fun navigate(tab: PortalTab) { selectedTab = tab }
}

@Composable
private fun PortalRoot(vm: PortalViewModel = viewModel()) {
    if (!vm.signedIn) LoginScreen(vm) else PortalScaffold(vm)
}

@Composable
private fun LoginScreen(vm: PortalViewModel) {
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Canvas).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(68.dp).clip(RoundedCornerShape(22.dp)).background(Green), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.School, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Text("JUET Portal", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Ink)
            Text("Your campus life, in one place.", color = Muted)
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Student sign in", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = id, onValueChange = { id = it }, label = { Text("Demo student ID") },
                        leadingIcon = { Icon(Icons.Default.Badge, null) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, label = { Text("Demo password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    vm.loginError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { vm.demoSignIn(id, password) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) {
                        Text("Preview dashboard")
                    }
                    Text(
                        "Demo mode only. Do not enter your real JUET credentials. The app is not connected to university services.",
                        color = Muted, style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text("Independent UI prototype · v0.1.0", color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PortalScaffold(vm: PortalViewModel) {
    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf(PortalTab.Home, PortalTab.Attendance, PortalTab.Results, PortalTab.Timetable, PortalTab.Profile).forEach { tab ->
                    NavigationBarItem(
                        selected = vm.selectedTab == tab,
                        onClick = { vm.navigate(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (vm.selectedTab) {
                PortalTab.Home -> DashboardScreen(vm)
                PortalTab.Attendance -> AttendanceScreen()
                PortalTab.Results -> ResultsScreen()
                PortalTab.Timetable -> TimetableScreen()
                PortalTab.Notices -> NoticesScreen()
                PortalTab.Profile -> ProfileScreen(vm)
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
            if (subtitle != null) Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        trailing?.invoke()
    }
}

@Composable
private fun DashboardScreen(vm: PortalViewModel) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            PageHeader("Good evening", "Here's your campus snapshot", trailing = {
                IconButton(onClick = { vm.navigate(PortalTab.Notices) }) { Icon(Icons.Default.NotificationsNone, "Notices") }
            })
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("STUDENT OVERVIEW", color = Color(0xFFB8C7D9), style = MaterialTheme.typography.labelMedium)
                        Text("Welcome back", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Student ID: DEMO-001", color = Color(0xFFE1E8F0))
                        Text("Academic data shown is illustrative", color = Color(0xFFB8C7D9), style = MaterialTheme.typography.bodySmall)
                    }
                    Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFF29415C)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.School, null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Attendance", "86%", "Across subjects", Icons.Default.CheckCircle, Modifier.weight(1f))
                MetricCard("CGPA", "8.42", "Sample result", Icons.Default.Grade, Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("Today's timetable", "VIEW ALL") { vm.navigate(PortalTab.Timetable) }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScheduleRow("09:00 AM", "Data Structures", "Room A-204", Green)
                ScheduleRow("11:00 AM", "Software Engineering", "Room B-106", Orange)
                ScheduleRow("02:00 PM", "Computer Networks", "Lab C-12", Color(0xFF5377C5))
            }
        }
        item {
            SectionTitle("Quick access", null, null)
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Attendance", Icons.Default.FactCheck, Modifier.weight(1f)) { vm.navigate(PortalTab.Attendance) }
                QuickAction("Results", Icons.Default.School, Modifier.weight(1f)) { vm.navigate(PortalTab.Results) }
                QuickAction("Notices", Icons.Default.Campaign, Modifier.weight(1f)) { vm.navigate(PortalTab.Notices) }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Green)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Prototype data", fontWeight = FontWeight.SemiBold, color = Green)
                        Text("Connect an approved university API before relying on academic information.", color = Ink, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, caption: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, tint = Green)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(caption, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String?, onAction: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun ScheduleRow(time: String, subject: String, room: String, accent: Color) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(46.dp).clip(RoundedCornerShape(4.dp)).background(accent))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(subject, fontWeight = FontWeight.SemiBold)
                Text(room, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(time, color = accent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(25.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

private data class AttendanceItem(val subject: String, val attended: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else attended * 100 / total
}
private val attendanceSample = listOf(
    AttendanceItem("Data Structures", 26, 30),
    AttendanceItem("Software Engineering", 24, 26),
    AttendanceItem("Computer Networks", 21, 27),
    AttendanceItem("Artificial Intelligence", 25, 28),
    AttendanceItem("Operating Systems", 22, 27)
)

@Composable
private fun AttendanceScreen() {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("Attendance", "Subject-wise overview · sample data") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Overall attendance", color = Color(0xFFB8C7D9))
                    Text("86%", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { 0.86f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = Color(0xFF66D5A9), trackColor = Color(0xFF3C4A61))
                    Text("Illustrative value — not an official record", color = Color(0xFFB8C7D9), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(attendanceSample) { item ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.subject, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text("${item.percent}%", color = if (item.percent < 75) Color(0xFFB54735) else Green, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = { item.percent / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = if (item.percent < 75) Color(0xFFCF6B58) else Green, trackColor = Canvas)
                    Text("${item.attended} attended out of ${item.total} classes", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ResultsScreen() {
    val rows = listOf("Data Structures" to "A", "Software Engineering" to "A-", "Computer Networks" to "B+", "Artificial Intelligence" to "A", "Operating Systems" to "B+")
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Results", "Semester overview · sample data") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Semester GPA", color = Color(0xFFB8C7D9))
                        Text("8.42", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("Illustrative result", color = Color(0xFFB8C7D9), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFF80DDB9), modifier = Modifier.size(42.dp))
                }
            }
        }
        item { Text("Subject grades", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(rows) { row ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.first, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Surface(color = PaleGreen, shape = RoundedCornerShape(10.dp)) {
                        Text(row.second, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Green, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class ClassItem(val time: String, val subject: String, val room: String, val faculty: String)
private val classesSample = listOf(
    ClassItem("09:00 AM", "Data Structures", "A-204", "Faculty details unavailable"),
    ClassItem("11:00 AM", "Software Engineering", "B-106", "Faculty details unavailable"),
    ClassItem("02:00 PM", "Computer Networks", "Lab C-12", "Faculty details unavailable"),
    ClassItem("03:30 PM", "Artificial Intelligence", "A-112", "Faculty details unavailable")
)

@Composable
private fun TimetableScreen() {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Timetable", "Today's schedule · sample data") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MON", "TUE", "WED", "THU", "FRI").forEachIndexed { index, day ->
                    Surface(modifier = Modifier.weight(1f), color = if (index == 0) Green else Color.White, shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day, color = if (index == 0) Color.White else Muted, style = MaterialTheme.typography.labelSmall)
                            Text("${13 + index}", color = if (index == 0) Color.White else Ink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        items(classesSample) { item ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(88.dp)) {
                        Text(item.time, color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(item.room, color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(item.subject, fontWeight = FontWeight.SemiBold)
                        Text(item.faculty, color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private data class NoticeItem(val title: String, val detail: String, val category: String)
private val noticesSample = listOf(
    NoticeItem("Welcome to the portal prototype", "This notice is sample content and is not issued by JUET.", "APP"),
    NoticeItem("Examination updates", "Official notices will appear here after an authorized backend is connected.", "ACADEMIC"),
    NoticeItem("Timetable updates", "Check official university channels for the current timetable.", "CAMPUS")
)

@Composable
private fun NoticesScreen() {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Notices", "Updates and announcements") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Green)
                    Spacer(Modifier.width(10.dp))
                    Text("Sample notices only. No official university feed is connected.", color = Ink, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(noticesSample) { notice ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(notice.category, color = Green, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(notice.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(notice.detail, color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(vm: PortalViewModel) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("My profile", "Account and preferences") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(76.dp).clip(CircleShape).background(PaleGreen), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Green, modifier = Modifier.size(42.dp))
                    }
                    Text("Demo Student", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Student ID: DEMO-001", color = Muted)
                    Text("Official profile data is not connected", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column {
                    ProfileLine(Icons.Default.School, "Institution", "Jaypee University of Engineering and Technology")
                    HorizontalDivider(color = Canvas)
                    ProfileLine(Icons.Default.Security, "Authentication", "Demo mode")
                    HorizontalDivider(color = Canvas)
                    ProfileLine(Icons.Default.CloudOff, "Backend", "Not connected")
                }
            }
        }
        item {
            OutlinedButton(onClick = { vm.signOut() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp)) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign out of demo")
            }
        }
    }
}

@Composable
private fun ProfileLine(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Green)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
