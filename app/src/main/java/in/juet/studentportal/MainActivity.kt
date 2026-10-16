package `in`.juet.studentportal

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private val Ink = Color(0xFFE7EDF5)
private val Green = Color(0xFF69D7B0)
private val PaleGreen = Color(0xFF183A35)
private val Canvas = Color(0xFF0B1016)
private val CardBg = Color(0xFF151D27)
private val Muted = Color(0xFFA0AEC0)
private val Orange = Color(0xFFF3B45E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PortalTheme { PortalRoot() } }
    }
}

@Composable
private fun PortalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Green, onPrimary = Color(0xFF06271F), secondary = Color(0xFF9CB8D8),
            background = Canvas, surface = CardBg, onSurface = Ink, onBackground = Ink,
            surfaceVariant = Color(0xFF222D3A), onSurfaceVariant = Muted,
            outline = Color(0xFF394655)
        ),
        content = content
    )
}

enum class PortalTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Attendance("Attendance", Icons.Default.CheckCircle),
    Results("Results", Icons.Default.School),
    Timetable("Timetable", Icons.Default.CalendarMonth),
    Notices("Notices", Icons.Default.Notifications),
    Website("JUET Info", Icons.Default.Language),
    PdfTimetable("S7 PDF", Icons.Default.CalendarMonth),
    Curriculum("Curriculum", Icons.Default.MenuBook),
    AcademicCalendar("Calendar PDF", Icons.Default.EventNote),
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
            Image(
                painter = painterResource(id = R.drawable.juet_logo),
                contentDescription = "Official JUET logo",
                modifier = Modifier.size(112.dp)
            )
            Text("JUET Portal", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Ink)
            Text("Your campus life, in one place.", color = Muted)
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
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
            NavigationBar(containerColor = CardBg) {
                listOf(PortalTab.Home, PortalTab.Attendance, PortalTab.Results, PortalTab.Timetable, PortalTab.Profile).forEach { tab ->
                    NavigationBarItem(
                        selected = vm.selectedTab == tab,
                        onClick = { vm.navigate(tab) },
                        icon = {
                            val iconScale by animateFloatAsState(
                                targetValue = if (vm.selectedTab == tab) 1.12f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "navigation_icon_scale"
                            )
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                            )
                        },
                        label = { Text(tab.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = vm.selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) +
                        slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(220))) togetherWith
                        (fadeOut(animationSpec = tween(140)) +
                            slideOutVertically(targetOffsetY = { -it / 18 }, animationSpec = tween(140)))
                },
                label = "portal_tab_transition"
            ) { tab ->
                when (tab) {
                    PortalTab.Home -> DashboardScreen(vm)
                    PortalTab.Attendance -> AttendanceScreen()
                    PortalTab.Results -> ResultsScreen()
                    PortalTab.Timetable -> TimetableScreen()
                    PortalTab.Notices -> NoticesScreen()
                    PortalTab.Website -> WebsiteInfoScreen(vm)
                    PortalTab.PdfTimetable -> PdfTimetableScreen(vm)
                    PortalTab.Curriculum -> CurriculumScreen(vm)
                    PortalTab.AcademicCalendar -> PdfAcademicCalendarScreen(vm)
                    PortalTab.Profile -> ProfileScreen(vm)
                }
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
            PageHeader("JUET Student Portal", "B.Tech CSE · Semester VII", trailing = {
                IconButton(onClick = { vm.navigate(PortalTab.Notices) }) { Icon(Icons.Default.NotificationsNone, "Notices") }
            })
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF172B3A))) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("STUDENT OVERVIEW", color = Green, style = MaterialTheme.typography.labelMedium)
                        Text("Welcome back", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("B.Tech CSE · Semester VII", color = Ink)
                        Text("Timetable and calendar sourced from your uploaded PDFs", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFF29415C)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.School, null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Semester", "VII", "July–Dec 2026", Icons.Default.CheckCircle, Modifier.weight(1f))
                MetricCard("Courses", "15", "Timetable course list", Icons.Default.Grade, Modifier.weight(1f))
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
            SectionTitle("JUET & PDF resources", "OPEN", null)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("JUET website", Icons.Default.Language, Modifier.weight(1f)) { vm.navigate(PortalTab.Website) }
                    QuickAction("S7 timetable", Icons.Default.CalendarMonth, Modifier.weight(1f)) { vm.navigate(PortalTab.PdfTimetable) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("B.Tech curriculum", Icons.Default.MenuBook, Modifier.weight(1f)) { vm.navigate(PortalTab.Curriculum) }
                    QuickAction("Academic calendar", Icons.Default.EventNote, Modifier.weight(1f)) { vm.navigate(PortalTab.AcademicCalendar) }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Green)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("University data added", fontWeight = FontWeight.SemiBold, color = Green)
                        Text("Academic calendar and Semester VII timetable are from your PDFs. Attendance and results still need official portal data.", color = Ink, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, caption: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
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
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
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
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
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
        item { PageHeader("Attendance", "Official attendance is not connected yet") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF172B3A)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sample attendance (not official)", color = Muted)
                    Text("86%", color = Ink, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    val attendanceProgress by animateFloatAsState(targetValue = 0.86f, animationSpec = tween(durationMillis = 900, delayMillis = 120), label = "attendance_summary_progress")
                    LinearProgressIndicator(progress = { attendanceProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = Green, trackColor = Color(0xFF3C4A61))
                    Text("Illustrative value — not an official record", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(attendanceSample) { item ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.subject, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text("${item.percent}%", color = if (item.percent < 75) Color(0xFFB54735) else Green, fontWeight = FontWeight.Bold)
                    }
                    val subjectProgress by animateFloatAsState(
                        targetValue = item.percent / 100f,
                        animationSpec = tween(durationMillis = 850, delayMillis = 100),
                        label = "subject_attendance_progress"
                    )
                    LinearProgressIndicator(progress = { subjectProgress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = if (item.percent < 75) Color(0xFFCF6B58) else Green, trackColor = Color(0xFF26313E))
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
        item { PageHeader("Results", "Official results are not connected yet") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF172B3A))) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sample GPA (not official)", color = Muted)
                        Text("8.42", color = Ink, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("Illustrative result", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFF80DDB9), modifier = Modifier.size(42.dp))
                }
            }
        }
        item { Text("Subject grades", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(rows) { row ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
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
    ClassItem("09:00 AM", "3D Printing (ME350)", "LT14", "Dr. YK Modi · Monday"),
    ClassItem("09:00 AM", "Knowledge Management (HS303)", "LT9", "Dr. Sandeep Srivastav · Monday"),
    ClassItem("10:00 AM", "Image Processing (CS308)", "LT4", "Prof. Vipin Tyagi · Monday"),
    ClassItem("11:00 AM", "Software Quality Management (CS304)", "LT11", "Dr. Dinesh Verma · Monday"),
    ClassItem("12:00 PM", "Large Language Models & GenAI (CS310)", "LT1", "Dr. Malti Nagle · Monday"),
    ClassItem("09:00 AM", "Legal & Ethical Issues in Business (HS011)", "LT1", "Dr. Sandeep Arya · Tuesday, Batch 2"),
    ClassItem("10:00 AM", "Deep Learning (CS302)", "LT1", "Prof. Mahesh Kumar (BX/BY), Dr. Ankur Mudgal (BZ) · Tuesday"),
    ClassItem("11:00 AM", "Introduction to Financial Management (HS009)", "LT11", "Dr. Shekhar Singh · Tuesday, Batch 1"),
    ClassItem("12:00 PM", "Applied Accelerated Deep Learning (CS307)", "LT12", "Dr. Ankur Mudgal · Tuesday"),
    ClassItem("03:00 PM", "Image Processing (CS308)", "LT11", "Wednesday, Batch 2")
)

@Composable
private fun TimetableScreen() {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Timetable", "B.Tech VII Semester · July–December 2026") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MON", "TUE", "WED", "THU", "FRI").forEachIndexed { index, day ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = if (index == 0) Green else CardBg,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day, color = if (index == 0) Color(0xFF06271F) else Muted, style = MaterialTheme.typography.labelSmall)
                            Text("${13 + index}", color = if (index == 0) Color(0xFF06271F) else Ink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        items(classesSample) { item ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(18.dp)) {
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


@Composable
private fun WebsiteInfoScreen(vm: PortalViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("JUET public information", "Useful official information · public website feed check every minute") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Latest notices", color = Green, fontWeight = FontWeight.Bold)
                    Text("This screen links to the official JUET source. The app checks its mirrored feed every 60 seconds; the mirror refreshes when the scheduled GitHub Actions sync succeeds.", color = Ink, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { ResourceLinkCard("Announcements & results", "Exam results, fee/registration notices, hostel notices and internships", "https://www.juet.ac.in/jaypee/announcement") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
        item { ResourceLinkCard("Academic calendar", "Semester schedule, examinations and holidays", "https://www.juet.ac.in/jaypee/Acalendar") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
        item { ResourceLinkCard("Scholarships", "Published scholarship eligibility and conditions", "https://www.juet.ac.in/jaypee/UniversityScholarship") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
        item { ResourceLinkCard("Placements & internships", "Placement cell information and career announcements", "https://www.juet.ac.in/jaypee/training-placement-cell") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
        item { ResourceLinkCard("Faculty and contact directory", "Official faculty details, phone numbers and email", "https://www.juet.ac.in/jaypee/contactus") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
        item { ResourceLinkCard("Student Web Kiosk", "Open the university student portal (official login)", "https://webkiosk.juet.ac.in/") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
        item { ResourceLinkCard("JUET home page", "Open the official university website", "https://www.juet.ac.in/") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
        item {
            OutlinedButton(onClick = { vm.navigate(PortalTab.Notices) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Icon(Icons.Default.Notifications, null)
                Spacer(Modifier.width(8.dp))
                Text("Open live notices feed")
            }
        }
    }
}

@Composable
private fun PdfTimetableScreen(vm: PortalViewModel) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Semester VII timetable", "From S7.pdf · July–December 2026") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Timetable reference", color = Green, fontWeight = FontWeight.Bold)
                    Text("B.Tech VII Semester · groups BX (B1–B4), BY (B5–B8), BZ (B9–B12), plus Batch 1 (B1–B6) and Batch 2 (B7–B12).", color = Ink)
                    Text("Periods shown in the PDF run from 9:00 AM through 4:55 PM. Check the original timetable for the correct day, batch, group, lab and room before attending.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { SectionTitle("Course and faculty directory", null, null) }
        items(listOf(
            "CE332 · Introduction to Sustainability" to "Dr. V.S. Babu",
            "HS303 · Knowledge Management" to "Dr. Sandeep Srivastav",
            "MA301 · Operations Research" to "Dr. Divya Jain",
            "ME350 · 3D Printing" to "Dr. YK Modi",
            "HS009 · Introduction to Financial Management" to "Dr. Shekhar Singh",
            "HS011 · Legal and Ethical Issues in Business" to "Dr. Sandeep Arya",
            "HS005 · Academic Research and Paper Writing" to "Dr. Kedari Lal Dhaker",
            "CS302 · Deep Learning" to "Prof. Mahesh Kumar (BX/BY); Dr. Ankur Mudgal (BZ)",
            "CS308 · Image Processing" to "Prof. Vipin Tyagi",
            "CS306 · Transformer Models" to "Ms. Reenu Rajput",
            "CS305 · AI Enabled Software Engineering" to "Dr. Neha Sharma",
            "CS307 · Applied Accelerated Deep Learning" to "Dr. Ankur Mudgal",
            "CS310 · Large Language Models and Generative AI" to "Dr. Malti Nagle",
            "CS311 · Generative AI" to "Ms. Sarita Sahni",
            "CS304 · Software Quality Management" to "Dr. Dinesh Verma"
        )) { row ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(row.first, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(row.second, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("The PDF's timetable uses short course labels and batch-specific room codes. This screen summarizes the course/faculty list; use the original PDF for the full grid and exact daily periods.", Modifier.padding(horizontal = 20.dp), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            OutlinedButton(onClick = { vm.navigate(PortalTab.Timetable) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text("Open timetable screen") }
        }
    }
}

@Composable
private fun CurriculumScreen(vm: PortalViewModel) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("B.Tech CSE curriculum", "From B.T.pdf · 8-semester course structure") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Program overview", color = Green, fontWeight = FontWeight.Bold)
                    Text("The uploaded curriculum lists 160 total credits for the B.Tech CSE program, including theory, laboratories, electives, value-added courses, training and projects.", color = Ink)
                    Text("Value-added courses do not count toward CGPA. The document notes optional industrial training after Semester II, four weeks after Semester IV, and six weeks after Semester VI (evaluated in Semester VII).", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { SectionTitle("Semester-wise subjects", null, null) }
        items(listOf(
            "Semester I" to "Engineering Mathematics, Engineering Physics-I, English, Computer Programming, Physics Lab-I, Computer Programming Lab, Workshop Practices.",
            "Semester II" to "Life Skills and Effective Communication, Discrete Mathematics, Engineering Physics-II, Electrical Science, Object Oriented Programming and associated labs.",
            "Semester III" to "Techniques for Decision Making, Data Structures, Digital Systems and Microprocessors, Database Systems, Environmental Science, UI/UX Lab and programming labs.",
            "Semester IV" to "HSS Elective-I, Software Engineering, Algorithms and Problem Solving, Computer Organisation and Architecture, Machine Learning and labs.",
            "Semester V" to "HSS Elective-2, Probability Theory and Random Processes, Discipline Elective-1, Computer Networks, Theory of Computation, Science Elective, Minor Project-1 and labs.",
            "Semester VI" to "HSS Elective-3, Operating Systems, Compiler Design, Discipline Elective-2, Minor Project-2 and labs.",
            "Semester VII" to "Open Elective-1, CS Electives 3–5, Major Project Part-I and Summer Training Viva.",
            "Semester VIII" to "Open Elective-2, CS Electives 6–7 and Major Project Part-II."
        )) { row ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(row.first, fontWeight = FontWeight.Bold, color = Green)
                    Text(row.second, color = Ink, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Text("Use the original curriculum PDF for exact course codes, L/T/P contact hours, credit values and elective choices; the summaries above are extracted highlights, not a replacement for the official document.", Modifier.padding(horizontal = 20.dp), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            OutlinedButton(onClick = { vm.navigate(PortalTab.PdfTimetable) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text("View Semester VII course/faculty list") }
        }
    }
}

@Composable
private fun PdfAcademicCalendarScreen(vm: PortalViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("Academic calendar", "From SY.pdf · semester planning") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Calendar highlights", color = Green, fontWeight = FontWeight.Bold)
                    Text("Use the uploaded academic calendar as the reference for semester dates, examination periods, holidays and university deadlines.", color = Ink)
                    Text("The app currently summarizes key events from the calendar; verify changes against the latest official JUET announcement before relying on a date.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { SectionTitle("Semester VII · July–December 2026", null, null) }
        items(listOf(
            "12–17 October 2026" to "Test 2 (T-2)",
            "29–31 October 2026" to "Proposed Technical Fest (JYC)",
            "6–14 November 2026" to "Mid-semester / Deepawali break",
            "30 November–5 December 2026" to "Makeup test and Practical Examination P-2",
            "7–12 December 2026" to "End-semester examination (T-3)",
            "19 December 2026" to "Deadline for evaluated T-3 scripts and result analysis",
            "21 December 2026" to "Result finalization and semester closure",
            "21 December 2026–3 January 2027" to "Winter vacation",
            "5 January 2027" to "Next semester classes commence"
        )) { row ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(row.second, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(row.first, color = Green, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.Event, null, tint = Green)
                }
            }
        }
        item {
            Text("These dates come from the currently loaded academic-calendar notes. Confirm the latest official PDF/notices in case the university revises the schedule.", Modifier.padding(horizontal = 20.dp), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            ResourceLinkCard("Open official JUET calendar", "Check the university's latest published calendar", "https://www.juet.ac.in/jaypee/Acalendar") { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }
}

@Composable
private fun ResourceLinkCard(title: String, detail: String, url: String, onOpen: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.OpenInNew, null, tint = Green, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onOpen(url) }) { Icon(Icons.Default.ChevronRight, "Open") }
        }
    }
}

private data class NoticeItem(val title: String, val detail: String, val category: String, val url: String = "")
private val noticesSample = listOf(
    NoticeItem("Test 2 (T-2)", "12–17 October 2026", "EXAM"),
    NoticeItem("Proposed Technical Fest (JYC)", "29–31 October 2026", "CAMPUS"),
    NoticeItem("Mid-semester / Deepawali break", "6–14 November 2026", "HOLIDAY"),
    NoticeItem("Makeup test and Practical Examination P-2", "30 November–5 December 2026", "EXAM"),
    NoticeItem("End-semester examination (T-3)", "7–12 December 2026", "EXAM"),
    NoticeItem("Last date for evaluated T-3 answer scripts and result analysis", "19 December 2026", "ACADEMIC"),
    NoticeItem("Result finalization and declaration by Registrar", "21 December 2026", "RESULT"),
    NoticeItem("Semester declared closed", "21 December 2026", "ACADEMIC"),
    NoticeItem("Winter vacation for students", "21 December 2026–3 January 2027", "HOLIDAY"),
    NoticeItem("Supplementary exam registration", "1 January 2027", "EXAM"),
    NoticeItem("Supplementary examination schedule", "2–5 January 2027", "EXAM"),
    NoticeItem("Supplementary result declaration", "6 January 2027", "RESULT"),
    NoticeItem("Next semester classes commence", "5 January 2027", "ACADEMIC"),
    NoticeItem("Independence Day", "15 August 2026", "HOLIDAY"),
    NoticeItem("Raksha Bandhan", "28 August 2026", "HOLIDAY"),
    NoticeItem("Janmashtami", "4 September 2026", "HOLIDAY"),
    NoticeItem("Mahatma Gandhi Jayanti", "2 October 2026", "HOLIDAY"),
    NoticeItem("Dussehra", "20 October 2026", "HOLIDAY"),
    NoticeItem("Deepawali", "8 November 2026", "HOLIDAY"),
    NoticeItem("Govardhan Pooja", "9 November 2026", "HOLIDAY"),
    NoticeItem("Christmas Day", "25 December 2026", "HOLIDAY")
)

@Composable
private fun NoticesScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var liveNotices by remember { mutableStateOf<List<NoticeItem>>(emptyList()) }
    var lastUpdated by remember { mutableStateOf("Loading latest university updates…") }

    LaunchedEffect(Unit) {
        while (true) {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(BuildConfig.PUBLIC_INFO_URL)
                        .openConnection()
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.setRequestProperty("Cache-Control", "no-cache")
                    val body = connection.getInputStream().bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val announcements = json.optJSONArray("announcements")
                    val parsed = buildList {
                        if (announcements != null) {
                            for (i in 0 until announcements.length()) {
                                val item = announcements.optJSONObject(i) ?: continue
                                add(NoticeItem(
                                    title = item.optString("title", "University announcement"),
                                    detail = item.optString("date", item.optString("summary", "Official JUET website update")),
                                    category = "OFFICIAL",
                                    url = item.optString("url", "")
                                ))
                            }
                        }
                    }
                    parsed to json.optString("updatedAt", "Recently checked")
                }
            }
            result.onSuccess { (items, updated) ->
                liveNotices = items
                lastUpdated = "Feed checked: " + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()) +
                    " · Source sync: $updated · checks every 1 min"
            }.onFailure {
                lastUpdated = "Could not refresh feed. Last checked: " +
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()) +
                    " · Official website links remain available."
            }
            delay(60_000)
        }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("JUET updates", "Public announcements and useful links") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(lastUpdated, color = Green, style = MaterialTheme.typography.labelMedium)
                    Text("Announcements are fetched from the public JUET website index mirrored by this project's GitHub Actions sync (scheduled every five minutes). Private student attendance, marks, fees and profile records are not included.", color = Ink, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (liveNotices.isEmpty()) {
            item {
                Text("Recent public announcements", Modifier.padding(horizontal = 20.dp), color = Muted, style = MaterialTheme.typography.titleMedium)
            }
            items(noticesSample) { notice ->
                NoticeCard(notice) { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        } else {
            items(liveNotices) { notice ->
                NoticeCard(notice) { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }
        item {
            Text("Official JUET links", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(18.dp)) {
                Column {
                    OfficialLinkRow("Academic calendar", "juet.ac.in/jaypee/Acalendar", "https://www.juet.ac.in/jaypee/Acalendar") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    HorizontalDivider(color = Color(0xFF26313E))
                    OfficialLinkRow("Announcements & results", "Latest notices from JUET", "https://www.juet.ac.in/jaypee/Announcement") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    HorizontalDivider(color = Color(0xFF26313E))
                    OfficialLinkRow("Curriculum", "Programs and course curriculum", "https://www.juet.ac.in/") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    HorizontalDivider(color = Color(0xFF26313E))
                    OfficialLinkRow("Student web kiosk", "webkiosk.juet.ac.in", "https://webkiosk.juet.ac.in/") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    HorizontalDivider(color = Color(0xFF26313E))
                    OfficialLinkRow("University webmail", "webmail.juet.ac.in", "https://webmail.juet.ac.in/") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    HorizontalDivider(color = Color(0xFF26313E))
                    OfficialLinkRow("Contact JUET", "Phone, email and campus address", "https://www.juet.ac.in/jaypee/contactus") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                }
            }
        }
        item {
            Text("Contact information", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("JUET, A-B Road, Raghogarh, District Guna, Madhya Pradesh 473226", color = Ink)
                    Text("Main phone: +91 7544 267051 / 267310 / 267311 / 267312", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text("Admission helpline: +91 8982129100", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text("Registrar office: +91 8982129108", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text("Email: contact@juet.ac.in", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text("Admission email: admissions@juet.ac.in", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(notice: NoticeItem, onOpen: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(notice.category, color = Green, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(notice.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(notice.detail, color = Muted, style = MaterialTheme.typography.bodyMedium)
            if (notice.url.startsWith("https://")) {
                TextButton(onClick = { onOpen(notice.url) }) { Text("Open official notice") }
            }
        }
    }
}

@Composable
private fun OfficialLinkRow(title: String, subtitle: String, url: String, onOpen: (String) -> Unit) {
    TextButton(onClick = { onOpen(url) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProfileScreen(vm: PortalViewModel) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("My profile", "Student details and app status") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(22.dp)) {
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
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(18.dp)) {
                Column {
                    ProfileLine(Icons.Default.School, "Institution", "Jaypee University of Engineering and Technology")
                    HorizontalDivider(color = Color(0xFF26313E))
                    ProfileLine(Icons.Default.Security, "Authentication", "Demo mode")
                    HorizontalDivider(color = Color(0xFF26313E))
                    ProfileLine(Icons.Default.Cloud, "Public data feed", "Connected · official public JUET links")
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
