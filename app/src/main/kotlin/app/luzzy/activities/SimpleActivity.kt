package app.luzzy.activities

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.WindowInsetsControllerCompat
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.extensions.getProperBackgroundColor
import com.goodwy.commons.extensions.getProperPrimaryColor
import com.goodwy.commons.extensions.getProperTextColor
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import app.luzzy.R

open class SimpleActivity : BaseSimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        clearForcedBrandColors()
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun clearForcedBrandColors() {
        // Eliminar los colores naranja que escribíamos antes; goodwy-commons usará su azul por defecto
        getSharedPreferences("Prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("primary_color_2")
            .remove("primary_color")
            .apply()
    }

    override fun onStart() {
        super.onStart()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onResume() {
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        applyBrandTheme(window.decorView as? ViewGroup ?: return)
        // Postear para correr DESPUÉS de cualquier Handler de goodwy-commons
        window.decorView.post { forceStatusBarColor() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) forceStatusBarColor()
    }

    private fun forceStatusBarColor() {
        val bgColor = getProperBackgroundColor()
        window.statusBarColor = bgColor
        val isLight = (Color.red(bgColor) * 299 + Color.green(bgColor) * 587 + Color.blue(bgColor) * 114) / 1000 >= 128
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
    }

    private fun applyBrandTheme(viewGroup: ViewGroup) {
        val bgColor = getProperBackgroundColor()
        val textColor = getProperTextColor()
        val primaryColor = getProperPrimaryColor()
        val primaryColorList = ColorStateList.valueOf(primaryColor)

        val iconColor = getProperTextColor()

        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            when {
                child is AppBarLayout -> {
                    child.setBackgroundColor(bgColor)
                    applyBrandTheme(child)
                }
                child is MaterialToolbar -> {
                    child.setBackgroundColor(bgColor)
                    child.setTitleTextColor(textColor)
                    child.navigationIcon?.setTint(iconColor)
                    applyBrandTheme(child)
                }
                child.tag == "brand_fill" && child is MaterialButton -> {
                    child.backgroundTintList = primaryColorList
                    child.setTextColor(Color.WHITE)
                }
                child.tag == "brand_text" && child is MaterialButton -> {
                    child.setTextColor(primaryColor)
                    if (child.strokeWidth > 0) child.strokeColor = primaryColorList
                }
                child.tag == "brand_text" && child is TextView && child !is EditText -> {
                    child.setTextColor(primaryColor)
                }
                child is CardView -> {
                    child.setCardBackgroundColor(bgColor)
                    applyBrandTheme(child)
                }
                child is EditText -> {
                    child.setTextColor(textColor)
                    child.setHintTextColor(textColor and 0x00FFFFFF or 0x66000000)
                }
                child is ViewGroup -> applyBrandTheme(child)
            }
        }
    }

    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher,
        R.mipmap.ic_launcher_one,
        R.mipmap.ic_launcher_two,
        R.mipmap.ic_launcher_three,
        R.mipmap.ic_launcher_four,
        R.mipmap.ic_launcher_five,
        R.mipmap.ic_launcher_six,
        R.mipmap.ic_launcher_seven,
        R.mipmap.ic_launcher_eight,
        R.mipmap.ic_launcher_nine,
        R.mipmap.ic_launcher_ten,
        R.mipmap.ic_launcher_eleven
    )

    override fun getAppLauncherName() = getString(R.string.messages)

    override fun getRepositoryName() = "Messages"
}
