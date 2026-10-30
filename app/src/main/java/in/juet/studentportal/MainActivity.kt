package `in`.juet.studentportal

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
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
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.rgb(11, 16, 22)
        window.navigationBarColor = android.graphics.Color.rgb(11, 16, 22)
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
    StudentPortal("Live Portal", Icons.Default.Language),
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
    var selectedTab by mutableStateOf(PortalTab.Home)
        private set
    fun navigate(tab: PortalTab) { selectedTab = tab }
}

@Composable
private fun PortalRoot(vm: PortalViewModel = viewModel()) {
    var portalAuthenticated by remember { mutableStateOf(false) }
    if (!portalAuthenticated) {
        StudentPortalScreen(
            onAuthenticated = {
                portalAuthenticated = true
                vm.navigate(PortalTab.Home)
            },
            onLoggedOut = { portalAuthenticated = false }
        )
    } else {
        PortalScaffold(vm = vm, onLogout = {
            portalAuthenticated = false
            vm.navigate(PortalTab.StudentPortal)
        })
    }
}

@Composable
private fun PortalScaffold(vm: PortalViewModel, onLogout: () -> Unit) {
    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            NavigationBar(containerColor = CardBg) {
                listOf(PortalTab.Home, PortalTab.StudentPortal, PortalTab.Timetable, PortalTab.Notices, PortalTab.Profile).forEach { tab ->
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
                    PortalTab.StudentPortal -> StudentPortalScreen(onAuthenticated = {}, onLoggedOut = onLogout)
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
private fun StudentPortalScreen(onAuthenticated: () -> Unit, onLoggedOut: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val portalUrl = "https://studentportal.juet.ac.in/studentportal/"
    var userId by remember { mutableStateOf("231B26") }
    var captcha by remember { mutableStateOf("") }
    var captchaBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val loginScope = rememberCoroutineScope()
    var role by remember { mutableStateOf("Student") }
    var stage by remember { mutableStateOf("loading") }
    var status by remember { mutableStateOf("Connecting to the official JUET portal…") }
    var working by remember { mutableStateOf(false) }
    var inspectionAttempts by remember { mutableStateOf(0) }
    val loginFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF20242A),
        unfocusedTextColor = Color(0xFF20242A),
        focusedBorderColor = Color(0xFF8A929A),
        unfocusedBorderColor = Color(0xFFB0B5BA),
        focusedLabelColor = Color(0xFF59636E),
        unfocusedLabelColor = Color(0xFF66717E),
        cursorColor = Color(0xFF2196F3)
    )

    // Inspect the top-level page and accessible same-origin frames. Field order is
    // used only as a fallback for the known JUET layout: User ID, then CAPTCHA.
    val inspectScript = """
      (function () {
        var docs = [document];
        for (var i = 0; i < window.frames.length; i++) {
          try {
            var frameDoc = window.frames[i].document;
            if (frameDoc && docs.indexOf(frameDoc) < 0) docs.push(frameDoc);
          } catch (ignored) {}
        }
        function visible(e) {
          if (!e) return false;
          var r = e.getBoundingClientRect ? e.getBoundingClientRect() : null;
          return !!(r && r.width > 0 && r.height > 0);
        }
        function labelText(e) {
          var parts = [e.name, e.id, e.placeholder, e.getAttribute('aria-label'), e.title];
          try {
            if (e.labels) Array.prototype.forEach.call(e.labels, function (l) { parts.push(l.innerText || l.textContent || ''); });
          } catch (ignored) {}
          try {
            var p = e.closest('label') || e.parentElement;
            if (p) parts.push(p.innerText || p.textContent || '');
          } catch (ignored) {}
          return parts.join(' ').toLowerCase();
        }
        var allText = [];
        var allInputs = [];
        var allImages = [];
        var allCanvases = [];
        var allButtons = [];
        docs.forEach(function (d) {
          try { allText.push((d.body && d.body.innerText) || ''); } catch (ignored) {}
          try { Array.prototype.push.apply(allInputs, Array.prototype.slice.call(d.querySelectorAll('input,textarea'))); } catch (ignored) {}
          try { Array.prototype.push.apply(allImages, Array.prototype.slice.call(d.images)); } catch (ignored) {}
          try { Array.prototype.push.apply(allCanvases, Array.prototype.slice.call(d.querySelectorAll('canvas'))); } catch (ignored) {}
          try { Array.prototype.push.apply(allButtons, Array.prototype.slice.call(d.querySelectorAll('button,input[type=submit],input[type=button],a[role=button]'))); } catch (ignored) {}
        });
        var body = allText.join(' ').toLowerCase();
        var fields = allInputs.filter(visible);
        var passwordField = fields.some(function (e) { return e.tagName === 'INPUT' && e.type === 'password'; });
        var editable = fields.filter(function (e) {
          if (e.tagName === 'TEXTAREA') return true;
          var t = (e.type || 'text').toLowerCase();
          return ['text','email','tel','number','search'].indexOf(t) >= 0;
        });
        var user = editable.find(function (e) { return /user id|user.?id|roll|enroll|student.?id|login.?id|registration/i.test(labelText(e)); }) || editable[0];
        var cap = editable.find(function (e) { return /captcha|verification|security code|text as shown/i.test(labelText(e)); }) || editable.find(function (e) { return e !== user; });
        var loginButton = allButtons.some(function (e) { return /login|log in|sign in|continue/i.test((e.innerText || e.value || e.textContent || '').trim()); });
        var loginHeading = /sign in to your account|sign in|student\s+parent/.test(body);
        var loginForm = !passwordField && ((!!user && !!cap && loginButton) || (editable.length >= 2 && loginButton) || (loginHeading && editable.length >= 2));
        var captchaImages = allImages.filter(function (im) { return visible(im) && im.naturalWidth > 60 && im.naturalHeight > 12; });
        var captchaImage = captchaImages.find(function (im) {
          return /captcha|verify|security|validation/i.test((im.src || '') + ' ' + (im.alt || '') + ' ' + (im.id || '') + ' ' + (im.className || ''));
        }) || captchaImages.find(function (im) {
          var ratio = im.naturalWidth / Math.max(1, im.naturalHeight);
          return im.naturalWidth >= 120 && im.naturalHeight <= 130 && ratio >= 1.8;
        });
        var captchaData = '';
        var captchaUrl = '';
        if (captchaImage) {
          captchaUrl = captchaImage.currentSrc || captchaImage.src || '';
          try {
            var canvas = captchaImage.ownerDocument.createElement('canvas');
            canvas.width = captchaImage.naturalWidth;
            canvas.height = captchaImage.naturalHeight;
            canvas.getContext('2d').drawImage(captchaImage, 0, 0);
            captchaData = canvas.toDataURL('image/png').split(',')[1] || '';
          } catch (ignored) {}
        }
        if (!captchaData && !captchaImage) {
          var canvasImage = allCanvases.find(function (c) {
            var r = c.getBoundingClientRect();
            return r.width >= 120 && r.height >= 15 && r.height <= 130;
          });
          if (canvasImage) {
            try { captchaData = canvasImage.toDataURL('image/png').split(',')[1] || ''; } catch (ignored) {}
          }
        }
        var unavailable = /bad gateway|temporarily unavailable|error 502|site can.t be reached/.test(body);
        var signedIn = /log ?out|sign ?out|student dashboard|my profile/.test(body) && !passwordField && !loginForm;
        var state = unavailable ? 'error' : (passwordField ? 'password' : (loginForm ? 'username' : (signedIn ? 'authenticated' : 'unknown')));
        return JSON.stringify({
          state: state,
          captcha: captchaData,
          captchaUrl: captchaUrl,
          inputCount: fields.length,
          editableCount: editable.length,
          buttonFound: loginButton,
          headingFound: loginHeading,
          bodyLength: body.length
        });
      })();
    """.trimIndent()

    fun inspectPortal(view: WebView?) {
        if (view == null) return
        view.evaluateJavascript(inspectScript) { raw ->
            val payload = runCatching { org.json.JSONTokener(raw).nextValue() as? String }.getOrNull()
                ?: return@evaluateJavascript
            val result = runCatching { org.json.JSONObject(payload) }.getOrNull()
                ?: return@evaluateJavascript
            when (result.optString("state")) {
                "error" -> {
                    stage = "error"
                    status = "JUET's server is temporarily unavailable. You can retry or open the same portal in your browser."
                }
                "username" -> {
                    stage = "username"
                    inspectionAttempts = 0
                    status = "Enter your user ID and the CAPTCHA shown by JUET."
                    val encoded = result.optString("captcha")
                    if (encoded.isNotBlank()) {
                        captchaBitmap = runCatching {
                            val bytes = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }.getOrNull()
                    } else {
                        val imageUrl = result.optString("captchaUrl")
                        if (imageUrl.startsWith("https://") || imageUrl.startsWith("http://")) {
                            loginScope.launch {
                                val downloaded = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val resolved = java.net.URL(java.net.URL(portalUrl), imageUrl)
                                        val connection = resolved.openConnection()
                                        connection.connectTimeout = 10000
                                        connection.readTimeout = 10000
                                        connection.setRequestProperty("Referer", portalUrl)
                                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                                        CookieManager.getInstance().getCookie(resolved.toString())?.let {
                                            connection.setRequestProperty("Cookie", it)
                                        }
                                        connection.getInputStream().use { stream ->
                                            android.graphics.BitmapFactory.decodeStream(stream)
                                        }
                                    }.getOrNull()
                                }
                                if (downloaded != null) captchaBitmap = downloaded
                            }
                        }
                    }
                    if (captchaBitmap == null && inspectionAttempts < 8) {
                        inspectionAttempts += 1
                        view.postDelayed({ inspectPortal(view) }, 900)
                    }
                }
                "password" -> {
                    stage = "password"
                    status = "JUET accepted the first step. Enter your password on the official JUET page."
                }
                "authenticated" -> {
                    CookieManager.getInstance().flush()
                    onAuthenticated()
                }
                else -> {
                    inspectionAttempts += 1
                    if (inspectionAttempts < 16) {
                        view.postDelayed({ inspectPortal(view) }, 1000)
                    } else {
                        // An unrecognized/empty WebView is not a usable login screen.
                        // Show recovery actions instead of leaving a blank page with a spinner.
                        stage = "error"
                        status = if (result.optInt("bodyLength", 0) == 0) {
                            "JUET returned an empty page inside the app. The portal or one of its required services may not be responding. Retry or open the portal in your browser."
                        } else {
                            "The JUET page loaded, but its sign-in form could not be identified. Open the official page in your browser to continue."
                        }
                    }
                }
            }
        }
    }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.userAgentString = settings.userAgentString.replace("; wv", "").replace("Version/4.0 ", "")
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress == 100) view?.postDelayed({ inspectPortal(view) }, 350)
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    stage = "loading"
                    inspectionAttempts = 0
                    status = "Loading JUET sign-in…"
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    CookieManager.getInstance().flush()
                    view?.postDelayed({ inspectPortal(view) }, 600)
                }
                override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        stage = "error"
                        status = error?.description?.toString() ?: "Could not connect to JUET."
                    }
                }
                override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, response: android.webkit.WebResourceResponse?) {
                    if (request?.isForMainFrame == true && (response?.statusCode ?: 200) >= 400) {
                        stage = "error"
                        status = "JUET server returned an HTTP error."
                    }
                }
            }
            loadUrl(portalUrl)
        }
    }

    fun submitUsernameAndCaptcha() {
        working = true
        status = "Sending your entries to JUET…"
        val submitScript = """
          (function () {
            var docs = [document];
            for (var i = 0; i < window.frames.length; i++) {
              try {
                var fd = window.frames[i].document;
                if (fd && docs.indexOf(fd) < 0) docs.push(fd);
              } catch (ignored) {}
            }
            function visible(e) {
              var r = e && e.getBoundingClientRect ? e.getBoundingClientRect() : null;
              return !!(r && r.width > 0 && r.height > 0);
            }
            function labelText(e) {
              var p = [e.name,e.id,e.placeholder,e.getAttribute('aria-label'),e.title];
              try { if (e.labels) Array.prototype.forEach.call(e.labels,function(l){p.push(l.innerText||l.textContent||'');}); } catch (ignored) {}
              try { var l=e.closest('label')||e.parentElement;if(l)p.push(l.innerText||l.textContent||''); } catch (ignored) {}
              return p.join(' ').toLowerCase();
            }
            var fields=[];
            var buttons=[];
            docs.forEach(function(d){
              try { Array.prototype.push.apply(fields,Array.prototype.slice.call(d.querySelectorAll('input,textarea')).filter(visible)); } catch (ignored) {}
              try { Array.prototype.push.apply(buttons,Array.prototype.slice.call(d.querySelectorAll('button,input[type=submit],input[type=button],a[role=button]'))); } catch (ignored) {}
            });
            var editable=fields.filter(function(e){
              if(e.tagName==='TEXTAREA')return true;
              return ['text','email','tel','number','search',''].indexOf((e.type||'text').toLowerCase())>=0;
            });
            var user=editable.find(function(e){return /user id|user.?id|roll|enroll|student.?id|login.?id|registration/i.test(labelText(e));})||editable[0];
            var cap=editable.find(function(e){return /captcha|verification|security code|text as shown/i.test(labelText(e));})||editable.find(function(e){return e!==user;});
            var radios=[];
            docs.forEach(function(d){try{Array.prototype.push.apply(radios,Array.prototype.slice.call(d.querySelectorAll('input[type=radio]')));}catch(ignored){}});
            var chosen=radios.find(function(e){
              var text=(e.parentElement&&(e.parentElement.innerText||e.parentElement.textContent))||e.value||'';
              try { if(e.labels)Array.prototype.forEach.call(e.labels,function(l){text+=' '+(l.innerText||l.textContent||'');}); } catch(ignored){}
              return new RegExp(ROLE,'i').test(text);
            });
            function setValue(e,v){
              if(!e)return false;
              var proto=Object.getPrototypeOf(e),desc=Object.getOwnPropertyDescriptor(proto,'value');
              if(desc&&desc.set)desc.set.call(e,v);else e.value=v;
              e.dispatchEvent(new Event('input',{bubbles:true}));
              e.dispatchEvent(new Event('change',{bubbles:true}));
              return true;
            }
            if(chosen&&!chosen.checked)chosen.click();
            var ok=setValue(user,USER_VALUE)&&setValue(cap,CAPTCHA_VALUE);
            var button=buttons.find(function(e){return /login|log in|sign in|continue/i.test((e.innerText||e.value||e.textContent||'').trim());});
            if(ok&&button){button.click();return 'submitted';}
            if(ok&&user.form){if(user.form.requestSubmit)user.form.requestSubmit();else user.form.submit();return 'submitted';}
            return 'fields-not-found';
          })();
        """.trimIndent()
            .replace("ROLE", org.json.JSONObject.quote(role))
            .replace("USER_VALUE", org.json.JSONObject.quote(userId))
            .replace("CAPTCHA_VALUE", org.json.JSONObject.quote(captcha))
        webView.evaluateJavascript(submitScript) { result ->
            working = false
            if (result?.contains("submitted") == true) {
                stage = "loading"
                inspectionAttempts = 0
                status = "JUET is validating your user ID and CAPTCHA…"
                webView.postDelayed({ inspectPortal(webView) }, 700)
            } else {
                stage = "fallback"
                status = "The page fields differed from the native form. The official JUET page is open below; complete the CAPTCHA there."
            }
        }
    }

    BackHandler(enabled = (stage == "password" || stage == "fallback") && webView.canGoBack()) {
        webView.goBack()
    }

    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(Color(0xFFF7F8FA))
    ) {
        Row(
            Modifier.fillMaxWidth().background(Canvas).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.juet_launcher_user_logo),
                contentDescription = "JUET university logo",
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("JUET Student Portal", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Official university sign-in", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { stage = "loading"; inspectionAttempts = 0; webView.reload() }) {
                Icon(Icons.Default.Refresh, "Refresh", tint = Ink)
            }
            IconButton(onClick = {
                CookieManager.getInstance().removeAllCookies {
                    CookieManager.getInstance().flush()
                    WebStorage.getInstance().deleteAllData()
                    webView.clearHistory()
                    webView.clearCache(true)
                    onLoggedOut()
                    captcha = ""
                    captchaBitmap = null
                    stage = "loading"
                    inspectionAttempts = 0
                    webView.loadUrl(portalUrl)
                }
            }) { Icon(Icons.Default.Logout, "Log out", tint = Orange) }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize().alpha(if (stage == "password" || stage == "fallback") 1f else 0f)
            )
            when (stage) {
                "username" -> Column(
                    Modifier.fillMaxSize().background(Color.White)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(6.dp))
                    Text("SIGN IN TO YOUR ACCOUNT", color = Color(0xFF20242A), style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = role == "Student", onClick = { role = "Student" })
                            Text("Student", color = Color(0xFF303943))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = role == "Parent", onClick = { role = "Parent" })
                            Text("Parent", color = Color(0xFF303943))
                        }
                    }
                    OutlinedTextField(
                        userId, { userId = it },
                        label = { Text("USER ID*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = loginFieldColors
                    )
                    if (captchaBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = captchaBitmap!!.asImageBitmap(),
                            contentDescription = "CAPTCHA supplied by JUET",
                            modifier = Modifier.fillMaxWidth().height(78.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        Text("Fetching CAPTCHA from JUET…", color = Color(0xFF526171))
                    }
                    OutlinedTextField(
                        captcha, { captcha = it },
                        label = { Text("Enter the text as shown in the image *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = loginFieldColors
                    )
                    Button(
                        onClick = { submitUsernameAndCaptcha() },
                        enabled = !working && userId.isNotBlank() && captcha.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text(if (working) "PLEASE WAIT…" else "LOGIN") }
                    Text(status, color = Color(0xFF526171), style = MaterialTheme.typography.bodySmall)
                    Text(
                        "JUET validates the CAPTCHA. Password entry stays on JUET's official page.",
                        color = Color(0xFF718096),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Powered by JIL Information Technology Ltd.", color = Color(0xFF66717E), style = MaterialTheme.typography.labelSmall)
                }
                "loading" -> Column(
                    Modifier.fillMaxSize().background(Color.White),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Green)
                    Spacer(Modifier.height(12.dp))
                    Text(status, color = Color(0xFF26313E))
                }
                "error" -> Column(
                    Modifier.fillMaxSize().background(Color.White).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFF526171), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("JUET portal could not load", color = Color(0xFF182331), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(status, color = Color(0xFF526171))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { stage = "loading"; inspectionAttempts = 0; webView.loadUrl(portalUrl) }) { Text("Retry portal") }
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl))) }) {
                        Text("Open in Chrome / browser")
                    }
                }
                "fallback" -> Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                    color = Color(0xEE0B1016),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(14.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(vm: PortalViewModel) {
    val serviceGroups = listOf(
        "Student Information" to listOf("My Personal Information", "Student Medical Info", "Student Bank Detail"),
        "Academic & Fees" to listOf("Fee pay slip", "Fee Demand Report", "No Dues Status", "Hostel Allocation Details"),
        "Registration" to listOf("Subject Pre Registration", "Supplementary Subject Registration", "Summer Subject Registration", "GIP Subject Registration", "Subject Choice Print", "Registration Slip", "Supplementary Subject Choice Print"),
        "Attendance & Teaching" to listOf("My Class Attendance by Student", "Registered Subjects of Student With Faculty"),
        "Examinations" to listOf("My Examination Schedule", "View Event Subject Marks By Student", "Student Grade Card", "Student's Result", "Provision Certificate Acceptance"),
        "MOOC Subjects" to listOf("Subject Preference", "Subject Status"),
        "Feedback" to listOf("Faculty Feedback")
    )
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            PageHeader("JUET Student Portal", "University services in one place")
        }
        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF172B3A))
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Green, modifier = Modifier.size(34.dp))
                    Text("Your official student account", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Sign in directly on JUET's secure portal to view your own attendance, marks, fees, registration and profile. This app does not display invented student records.",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { vm.navigate(PortalTab.StudentPortal) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open official JUET login")
                    }
                }
            }
        }
        item {
            SectionTitle("Student services", null, null)
        }
        items(serviceGroups) { group ->
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(group.first, color = Green, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    group.second.forEach { service ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(service, color = Ink, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { vm.navigate(PortalTab.StudentPortal) }) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open official portal", tint = Green)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Public university resources", color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { vm.navigate(PortalTab.Notices) }, modifier = Modifier.fillMaxWidth()) { Text("Announcements and notices") }
                    OutlinedButton(onClick = { vm.navigate(PortalTab.Website) }, modifier = Modifier.fillMaxWidth()) { Text("Official JUET website and contacts") }
                    OutlinedButton(onClick = { vm.navigate(PortalTab.AcademicCalendar) }, modifier = Modifier.fillMaxWidth()) { Text("Academic calendar") }
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
private fun OfficialRecordNotice(title: String, message: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val portalUrl = "https://studentportal.juet.ac.in/studentportal/#/"
    Column(
        Modifier.fillMaxSize().background(Canvas).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageHeader(title, "Live information from the university")
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Green, modifier = Modifier.size(34.dp))
                Text("Official records only", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(message, color = Muted, style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl))) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open official JUET portal")
                }
            }
        }
    }
}

@Composable
private fun AttendanceScreen() {
    OfficialRecordNotice(
        title = "Attendance",
        message = "Your attendance is personal university data. Open the official JUET portal and select “My Class Attendance by Student” to view the live record."
    )
}
@Composable
private fun ResultsScreen() {
    OfficialRecordNotice(
        title = "Results & grade card",
        message = "Marks and grades are not stored in this app. Sign in to JUET and open “Student's Result” or “Student Grade Card” to view the official record."
    )
}
@Composable
private fun TimetableScreen() {
    OfficialRecordNotice(
        title = "Timetable",
        message = "This app does not invent class times or room assignments. Open the official JUET portal for your current registered subjects and attendance timetable. You can also use the university's published timetable PDF if your department has provided one."
    )
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
        item { PageHeader("Account", "Your personal data stays on the official portal") }
        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.size(76.dp).clip(CircleShape).background(PaleGreen), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Green, modifier = Modifier.size(42.dp))
                    }
                    Text("Student account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                    Text("No student identity is stored or fabricated by this app.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { vm.navigate(PortalTab.StudentPortal) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign in on official portal")
                    }
                }
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column {
                    ProfileLine(Icons.Default.School, "Institution", "Jaypee University of Engineering and Technology")
                    HorizontalDivider(color = Color(0xFF26313E))
                    ProfileLine(Icons.Default.Security, "Authentication", "Handled by official JUET portal")
                    HorizontalDivider(color = Color(0xFF26313E))
                    ProfileLine(Icons.Default.Lock, "Credentials", "Never entered into a separate app form")
                }
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
