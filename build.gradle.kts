name: Build APK

on:
  workflow_dispatch:
  push:
    branches: [main, master]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 8.9
      - name: Create project files
        run: |
          mkdir -p app app/src/main app/src/main/java/com/tripaccept/app app/src/main/res/values app/src/main/res/xml
          cat > settings.gradle.kts <<'EOF_FILE'
          pluginManagement {
              repositories {
                  google()
                  mavenCentral()
                  gradlePluginPortal()
              }
          }
          dependencyResolutionManagement {
              repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
              repositories {
                  google()
                  mavenCentral()
              }
          }
          rootProject.name = "TripAccept"
          include(":app")
          EOF_FILE
          cat > build.gradle.kts <<'EOF_FILE'
          plugins {
              id("com.android.application") version "8.5.2" apply false
              id("org.jetbrains.kotlin.android") version "1.9.24" apply false
          }
          EOF_FILE
          cat > gradle.properties <<'EOF_FILE'
          org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
          android.useAndroidX=false
          android.nonTransitiveRClass=true
          EOF_FILE
          cat > app/build.gradle.kts <<'EOF_FILE'
          plugins {
              id("com.android.application")
              id("org.jetbrains.kotlin.android")
          }

          android {
              namespace = "com.tripaccept.app"
              compileSdk = 34

              defaultConfig {
                  applicationId = "com.tripaccept.app"
                  minSdk = 24
                  targetSdk = 34
                  versionCode = 1
                  versionName = "1.0"
              }

              compileOptions {
                  sourceCompatibility = JavaVersion.VERSION_17
                  targetCompatibility = JavaVersion.VERSION_17
              }

              kotlinOptions {
                  jvmTarget = "17"
              }

              buildTypes {
                  release {
                      isMinifyEnabled = false
                  }
              }
          }
          EOF_FILE
          cat > app/src/main/AndroidManifest.xml <<'EOF_FILE'
          <?xml version="1.0" encoding="utf-8"?>
          <manifest xmlns:android="http://schemas.android.com/apk/res/android">

              <uses-permission android:name="android.permission.VIBRATE" />

              <application
                  android:allowBackup="false"
                  android:label="@string/app_name"
                  android:theme="@android:style/Theme.Material.Light">

                  <activity
                      android:name=".MainActivity"
                      android:exported="true">
                      <intent-filter>
                          <action android:name="android.intent.action.MAIN" />
                          <category android:name="android.intent.category.LAUNCHER" />
                      </intent-filter>
                  </activity>

                  <service
                      android:name=".AutoAcceptService"
                      android:exported="true"
                      android:label="@string/app_name"
                      android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
                      <intent-filter>
                          <action android:name="android.accessibilityservice.AccessibilityService" />
                      </intent-filter>
                      <meta-data
                          android:name="android.accessibilityservice"
                          android:resource="@xml/accessibility_service_config" />
                  </service>
              </application>
          </manifest>
          EOF_FILE
          cat > app/src/main/res/xml/accessibility_service_config.xml <<'EOF_FILE'
          <?xml version="1.0" encoding="utf-8"?>
          <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
              android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
              android:accessibilityFeedbackType="feedbackGeneric"
              android:accessibilityFlags="flagDefault|flagIncludeNotImportantViews"
              android:canRetrieveWindowContent="true"
              android:notificationTimeout="50"
              android:description="@string/service_desc" />
          EOF_FILE
          cat > app/src/main/res/values/strings.xml <<'EOF_FILE'
          <?xml version="1.0" encoding="utf-8"?>
          <resources>
              <string name="app_name">Trip Accept</string>
              <string name="service_desc">Uber आणि Rapido ड्रायव्हर ॲपमधील ट्रिप रिक्वेस्ट तुमच्या नियमांनुसार आपोआप Accept करते.</string>
          </resources>
          EOF_FILE
          cat > app/src/main/java/com/tripaccept/app/Prefs.kt <<'EOF_FILE'
          package com.tripaccept.app

          import android.content.Context
          import android.content.SharedPreferences

          object Prefs {
              const val DEFAULT_DROP_NAMES =
                  "Dombivli, Dombivali, डोंबिवली, Kalyan, कल्याण, Thane, ठाणे, Ghodbunder, घोडबंदर, " +
                      "Wagle, वागळे, Majiwada, Majiwade, माजिवडा, Kasarvadavali, Kasarvadvli, कासारवडवली, " +
                      "Kolshet, कोलशेत, Manpada, मानपाडा, Pakhadi, पाखाडी"

              private fun sp(c: Context): SharedPreferences =
                  c.getSharedPreferences("tripaccept", Context.MODE_PRIVATE)

              fun master(c: Context) = sp(c).getBoolean("master", false)
              fun setMaster(c: Context, v: Boolean) = sp(c).edit().putBoolean("master", v).apply()

              fun minFare(c: Context) = sp(c).getFloat("minFare", 80f).toDouble()
              fun setMinFare(c: Context, v: Double) = sp(c).edit().putFloat("minFare", v.toFloat()).apply()

              fun maxKm(c: Context) = sp(c).getFloat("maxKm", 2f).toDouble()
              fun setMaxKm(c: Context, v: Double) = sp(c).edit().putFloat("maxKm", v.toFloat()).apply()

              fun dropOn(c: Context) = sp(c).getBoolean("dropOn", true)
              fun setDropOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("dropOn", v).apply()

              fun dropNames(c: Context): String = sp(c).getString("dropNames", DEFAULT_DROP_NAMES) ?: DEFAULT_DROP_NAMES
              fun setDropNames(c: Context, v: String) = sp(c).edit().putString("dropNames", v).apply()

              fun vibrate(c: Context) = sp(c).getBoolean("vibrate", true)
              fun setVibrate(c: Context, v: Boolean) = sp(c).edit().putBoolean("vibrate", v).apply()

              fun last(c: Context): String = sp(c).getString("last", "") ?: ""
              fun setLast(c: Context, v: String) = sp(c).edit().putString("last", v).apply()

              fun dump(c: Context): String = sp(c).getString("dump", "") ?: ""
              fun setDump(c: Context, v: String) = sp(c).edit().putString("dump", v).apply()

              fun dropList(c: Context): List<String> =
                  dropNames(c).split(",", "\n").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
          }
          EOF_FILE
          cat > app/src/main/java/com/tripaccept/app/TripParser.kt <<'EOF_FILE'
          package com.tripaccept.app

          data class Trip(val fare: Double, val pickupKm: Double, val dropText: String)

          object TripParser {
              private val fareRe = Regex("""₹\s*([0-9][0-9,]*(?:\.[0-9]+)?)""")
              private val kmRe = Regex("""([0-9]+(?:\.[0-9]+)?)\s*km""", RegexOption.IGNORE_CASE)

              /**
               * texts: card मधील सर्व टेक्स्ट, स्क्रीनवरील क्रमाने.
               * पहिला km = पिकअप अंतर, दुसऱ्या km नंतरचा मजकूर = ड्रॉप लोकेशन.
               * (पिकअपचा पत्ता ड्रॉप म्हणून मोजला जाऊ नये म्हणून फक्त दुसऱ्या km नंतरचा भाग घेतो.)
               */
              fun parse(texts: List<String>): Trip? {
                  var fare: Double? = null
                  for (t in texts) {
                      val m = fareRe.find(t) ?: continue
                      fare = m.groupValues[1].replace(",", "").toDoubleOrNull()
                      if (fare != null) break
                  }
                  if (fare == null) return null

                  val kmIdx = texts.indices.filter { kmRe.containsMatchIn(texts[it]) }
                  if (kmIdx.isEmpty()) return null
                  val pickup = kmRe.find(texts[kmIdx[0]])?.groupValues?.get(1)?.toDoubleOrNull() ?: return null

                  val dropText = if (kmIdx.size >= 2) texts.drop(kmIdx[1] + 1).joinToString(" ") else ""
                  return Trip(fare, pickup, dropText)
              }
          }
          EOF_FILE
          cat > app/src/main/java/com/tripaccept/app/AutoAcceptService.kt <<'EOF_FILE'
          package com.tripaccept.app

          import android.accessibilityservice.AccessibilityService
          import android.content.Context
          import android.os.Build
          import android.os.SystemClock
          import android.os.VibrationEffect
          import android.os.Vibrator
          import android.view.accessibility.AccessibilityEvent
          import android.view.accessibility.AccessibilityNodeInfo
          import java.text.SimpleDateFormat
          import java.util.Date
          import java.util.Locale

          class AutoAcceptService : AccessibilityService() {

              private val acceptWords = setOf("accept", "confirm")
              private var lastScan = 0L
              private var lastClick = 0L
              private var lastDumpAt = 0L
              private var lastKey = ""

              override fun onInterrupt() {}

              override fun onAccessibilityEvent(event: AccessibilityEvent?) {
                  val pkg = event?.packageName?.toString() ?: return
                  if (!isTarget(pkg)) return
                  if (!Prefs.master(this)) return

                  val now = SystemClock.elapsedRealtime()
                  if (now - lastScan < 120) return
                  lastScan = now

                  val root = rootInActiveWindow ?: return
                  if (!isTarget(root.packageName?.toString() ?: "")) return
                  scan(root)
              }

              private fun isTarget(pkg: String) =
                  pkg.contains("rapido", ignoreCase = true) || pkg.contains("ubercab", ignoreCase = true)

              private fun label(n: AccessibilityNodeInfo): String =
                  (n.text?.toString() ?: n.contentDescription?.toString() ?: "").trim()

              private fun scan(root: AccessibilityNodeInfo) {
                  val buttons = ArrayList<AccessibilityNodeInfo>()
                  findButtons(root, buttons, 0)
                  if (buttons.isEmpty()) return

                  var parsedAny = false
                  for (b in buttons) {
                      val texts = cardTexts(b) ?: continue
                      val trip = TripParser.parse(texts) ?: continue
                      parsedAny = true
                      decide(b, trip, texts)
                  }

                  if (!parsedAny) {
                      val now = SystemClock.elapsedRealtime()
                      if (now - lastDumpAt > 3000) {
                          lastDumpAt = now
                          val all = ArrayList<String>()
                          collect(root, all, 0)
                          Prefs.setDump(this, "(ओळखू न आलेली स्क्रीन) " + all.joinToString(" | ").take(1500))
                      }
                  }
              }

              private fun findButtons(n: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo>, depth: Int) {
                  if (n == null || depth > 60) return
                  if (label(n).lowercase() in acceptWords) {
                      if (n.isVisibleToUser) out.add(n)
                      return
                  }
                  for (i in 0 until n.childCount) findButtons(n.getChild(i), out, depth + 1)
              }

              /** बटणाच्या वरच्या पातळ्यांमध्ये चढून, फक्त एकच Accept बटण असलेला सर्वात लहान कार्ड शोधतो. */
              private fun cardTexts(button: AccessibilityNodeInfo): List<String>? {
                  var node: AccessibilityNodeInfo? = button.parent
                  var depth = 0
                  while (node != null && depth < 8) {
                      val texts = ArrayList<String>()
                      val count = collect(node, texts, 0)
                      if (count > 1) return null
                      if (count == 1 && TripParser.parse(texts) != null) return texts
                      node = node.parent
                      depth++
                  }
                  return null
              }

              /** सर्व टेक्स्ट गोळा करतो; Accept/Confirm बटणांची संख्या परत देतो. */
              private fun collect(n: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int): Int {
                  if (n == null || depth > 60) return 0
                  val l = label(n)
                  if (l.lowercase() in acceptWords) {
                      out.add(l)
                      return 1
                  }
                  if (l.isNotEmpty()) out.add(l)
                  var c = 0
                  for (i in 0 until n.childCount) c += collect(n.getChild(i), out, depth + 1)
                  return c
              }

              private fun decide(button: AccessibilityNodeInfo, trip: Trip, texts: List<String>) {
                  val minFare = Prefs.minFare(this)
                  val maxKm = Prefs.maxKm(this)

                  val reason: String? = when {
                      trip.fare < minFare -> "भाडं कमी"
                      trip.pickupKm > maxKm -> "पिकअप लांब"
                      Prefs.dropOn(this) && !dropMatches(trip.dropText) -> "ड्रॉप यादीत नाही"
                      else -> null
                  }

                  val summary = "₹${trip.fare}, पिकअप ${trip.pickupKm} किमी, ड्रॉप: ${trip.dropText.take(70)}"
                  val key = summary + (reason ?: "OK")
                  val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                  if (reason != null) {
                      record(key, "$time वगळली ($reason): $summary", texts)
                      return
                  }

                  val now = SystemClock.elapsedRealtime()
                  if (now - lastClick < 2500) return
                  if (click(button)) {
                      lastClick = now
                      if (Prefs.vibrate(this)) buzz()
                      record(key + now, "$time Accept केली: $summary", texts)
                  } else {
                      record(key, "$time Accept बटण दाबता आलं नाही: $summary", texts)
                  }
              }

              private fun record(key: String, line: String, texts: List<String>) {
                  if (key == lastKey) return
                  lastKey = key
                  Prefs.setLast(this, line)
                  Prefs.setDump(this, texts.joinToString(" | ").take(1500))
              }

              private fun dropMatches(dropText: String): Boolean {
                  if (dropText.isBlank()) return false
                  val d = dropText.lowercase()
                  return Prefs.dropList(this).any { d.contains(it) }
              }

              private fun click(n: AccessibilityNodeInfo): Boolean {
                  var node: AccessibilityNodeInfo? = n
                  var i = 0
                  while (node != null && i < 5) {
                      if (node.isClickable && node.isEnabled) {
                          return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                      }
                      node = node.parent
                      i++
                  }
                  return false
              }

              @Suppress("DEPRECATION")
              private fun buzz() {
                  val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
                  if (Build.VERSION.SDK_INT >= 26) {
                      v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                  } else {
                      v.vibrate(300)
                  }
              }
          }
          EOF_FILE
          cat > app/src/main/java/com/tripaccept/app/MainActivity.kt <<'EOF_FILE'
          package com.tripaccept.app

          import android.app.Activity
          import android.content.Intent
          import android.os.Bundle
          import android.provider.Settings
          import android.text.InputType
          import android.widget.Button
          import android.widget.EditText
          import android.widget.LinearLayout
          import android.widget.ScrollView
          import android.widget.Switch
          import android.widget.TextView
          import android.widget.Toast

          class MainActivity : Activity() {

              private lateinit var swMaster: Switch
              private lateinit var etFare: EditText
              private lateinit var etKm: EditText
              private lateinit var swDrop: Switch
              private lateinit var etNames: EditText
              private lateinit var swVib: Switch
              private lateinit var tvStatus: TextView
              private lateinit var tvLast: TextView
              private lateinit var tvDump: TextView

              private fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

              override fun onCreate(savedInstanceState: Bundle?) {
                  super.onCreate(savedInstanceState)

                  val pad = (16 * resources.displayMetrics.density).toInt()
                  val root = LinearLayout(this).apply {
                      orientation = LinearLayout.VERTICAL
                      setPadding(pad, pad, pad, pad)
                  }

                  fun title(text: String) = TextView(this).apply {
                      this.text = text
                      textSize = 14f
                      setPadding(0, pad, 0, 0)
                  }

                  swMaster = Switch(this).apply {
                      text = "ऑटो Accept चालू"
                      textSize = 20f
                      isChecked = Prefs.master(this@MainActivity)
                  }
                  swMaster.setOnCheckedChangeListener { _, checked ->
                      Prefs.setMaster(this, checked)
                      if (checked && !serviceEnabled()) {
                          Toast.makeText(this, "आधी Accessibility सेवा चालू करा", Toast.LENGTH_LONG).show()
                      }
                  }
                  root.addView(swMaster)

                  tvStatus = TextView(this).apply { setPadding(0, pad / 2, 0, 0) }
                  root.addView(tvStatus)

                  root.addView(Button(this).apply {
                      text = "Accessibility सेटिंग उघडा"
                      setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                  })

                  root.addView(title("किमान भाडं (₹)"))
                  etFare = EditText(this).apply {
                      inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                      setText(fmt(Prefs.minFare(this@MainActivity)))
                  }
                  root.addView(etFare)

                  root.addView(title("कमाल पिकअप अंतर (किमी)"))
                  etKm = EditText(this).apply {
                      inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                      setText(fmt(Prefs.maxKm(this@MainActivity)))
    
