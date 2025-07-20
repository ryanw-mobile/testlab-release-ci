package com.rwmobi.githubcidemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.rwmobi.githubcidemo.ui.theme.GithubCIDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // allows your app to be edge-to-edge on Android 15 before.
        enableEdgeToEdge()

        setContent {
            GithubCIDemoTheme {
                // Android 15+ Will see solid red status bar
                // and red tinted navigation bar
                // Pre Android 15 will see tinted navigation bar
                // BUT status bar is not affected
                Surface(
                    modifier =
                    Modifier
                        .background(color = Color.Red) // for system bar area
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .background(color = MaterialTheme.colorScheme.background),
                    // for actual content background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GithubCIDemoTheme {
        Greeting("Android")
    }
}
