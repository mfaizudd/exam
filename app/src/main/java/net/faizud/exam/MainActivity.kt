package net.faizud.exam

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import net.faizud.exam.ui.ExamViewModel
import net.faizud.exam.ui.theme.ExamTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ExamViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        val sharedPreference =
            getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        viewModel = ExamViewModel(sharedPreference)
        viewModel.loadState()
        val webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = object: WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    viewModel.setLoading(true)
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    viewModel.setLoading(false)
                    super.onPageFinished(view, url)
                }
            }
            settings.javaScriptEnabled = true
        }
        loadHome(webView)
        setContent {
            ExamTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background,
                ) {
                    Scaffold(
                        floatingActionButtonPosition = FabPosition.End,
                        floatingActionButton = {
                            if (!viewModel.locked.value) {
                                FabMenu(webView, viewModel)
                            }
                        }
                    ) { padding ->
                        if (viewModel.locked.value) {
                            Login(Modifier.padding(padding), viewModel)
                        } else {
                            Exam(Modifier.padding(padding), webView)
                        }
                    }
                }
            }
        }
        hideSystemUI()
    }

    private fun hideSystemUI() {
        actionBar?.hide()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.lock()
        Log.d("DEBUG", "Paused")
    }

    override fun onResume() {
        super.onResume()
        Log.d("DEBUG", "Resumed")
    }
}

@Composable
fun FabMenu(webView: WebView, vm: ExamViewModel) {
    var show by remember { mutableStateOf(false) }
    val transition = updateTransition(show, label = "fab state")
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val offset by transition.animateDp(label = "fab offset") {
        when (it) {
            true -> 0.dp
            false -> 50.dp
        }
    }
    val visibility by transition.animateFloat(label = "fab visibility") {
        when (it) {
            true -> 1f
            false -> 0f
        }
    }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (visibility > 0f) {
            FloatingActionButton(
                modifier = Modifier
                    .padding(10.dp)
                    .offset(0.dp, offset.times(1.5f))
                    .alpha(visibility)
                    .size(45.dp),
                onClick = { loadHome(webView); show = !show }) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            }
            FloatingActionButton(
                modifier = Modifier
                    .padding(10.dp)
                    .offset(0.dp, offset)
                    .alpha(visibility)
                    .size(45.dp),
                onClick = { webView.reload(); show = !show }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .padding(10.dp),
            onClick = { show = !show }) {
            Crossfade(targetState = show) { shown ->
                when (shown) {
                    false -> {
                        if (vm.loading.value) {
                            Icon(
                                modifier = Modifier.rotate(rotation),
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Close options"
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Options"
                            )
                        }
                    }
                    true -> {
                        Icon(
                            modifier = Modifier.rotate(visibility * 180),
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close options"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Login(modifier: Modifier, viewModel: ExamViewModel) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextField(value = "", onValueChange = {}, placeholder = { Text("Password") })
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.unlock() }) {
            Text(text = "Submit")
        }
    }
}

fun loadHome(webView: WebView) {
    val url = "https://lms.faizud.net"
    webView.loadUrl(url)
}

@Composable
fun Exam(modifier: Modifier, webView: WebView) {
    AndroidView(factory = {
        webView
    })
}