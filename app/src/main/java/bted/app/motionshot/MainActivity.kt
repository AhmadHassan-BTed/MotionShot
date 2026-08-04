package bted.app.motionshot

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import bted.app.motionshot.ui.CameraScreen
import bted.app.motionshot.ui.theme.MotionShotTheme
import bted.app.motionshot.viewmodel.MotionShotViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force light system-bar icons on transparent background (dark UI).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            MotionShotTheme {
                val viewModel: MotionShotViewModel = viewModel()
                CameraScreen(viewModel = viewModel)
            }
        }
    }
}