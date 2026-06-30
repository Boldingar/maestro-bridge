```markdown
#Deployment Guide: Building & Installing to Phone and Watch

This project includes both a mobile app (`:app`) and a Wear OS watch app (`:wear`). Follow this quick guide to build the APKs and install them on your devices.

### Prerequisites
* Ensure **Developer Options** and **Debugging** are enabled on both your phone and watch.
* Connect both your computer and your watch to the **same Wi-Fi network**.

---

### Step 1: Build the APKs
1. Open the project in Android Studio.
2. In the top menu, go to **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
3. Once the build finishes, a notification will pop up in the bottom right corner. Click **Locate** to find your files, or find them manually at:
   * **Phone APK:** `app/build/outputs/apk/debug/bridgeApp.apk`
   * **Watch APK:** `wear/build/outputs/apk/debug/bridgeApp.apk`

---

### Step 2: Install onto the Phone
1. Connect your phone to your computer via USB.
2. In the Android Studio toolbar dropdown, select **`app`** and choose your physical phone.
3. Click the green **Run** button (or press `Shift + F10`) to install.

---

### Step 3: Install onto the Wear OS Watch
1. **Get the Watch IP:** On your watch, go to **Settings** > **Developer Options** > **Wireless Debugging**. Turn it on and note the IP and port (e.g., `192.168.1.50:5555`).
2. **Connect via Terminal:** Open the **Terminal** tab at the bottom of Android Studio and run (replace with your watch's IP):
   ```bash
   adb connect 192.168.1.50:5555

```
 3. **Install the Watch APK:** Run the following command in the terminal to push the app to your watch:
   ```bash
   adb install wear/build/outputs/apk/debug/bridgeApp.apk
   
   ```
 4. Once it says **Success**, the app will be available in your watch's app drawer!
```
These steps are subject to change based on the Android version of both your smartwatch and Android phone.
