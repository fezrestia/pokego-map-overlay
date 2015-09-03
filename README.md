# Ingress IntelMap Overlay
An Android application to enable overlay Intel Map on your scanner.
You can record your activity via Imtel Map screen shots.

## APPLICATION DESCRIPTION
This application realizes following functionalities.
- Overlay Intel Map on your scanner
- Capture screen shot of Imtel Map repeatedly (Manual/Auto)
- Store screen shots on local storage

## BUILD PROJECT DESCRIPTION
Project structure is based on Android Studio project<br>
Pre-Built/Signed APK is placed on modue root.

## HOW TO USE
1. Install the APK
2. Start "Ingress Intel Overlay"
3. Click to check "Enable overlay view", and overlay Intel Map is available<br>
   After then, you can use On-Screen UI functions
4. Click to check "Enable cycle record", and recording is started
5. Uncheck them to disable / stop.<br>
   Screen shots are stored in "&lt;root&gt;/IngressIntelOverlay/"

On-Screen UI
 - CAPTURE
   Capture current Intel Map screen shot manually.
 - RELOAD
   Reload Intem Map manually.

## PREFERENCES
### Property
#### Enable always reload
During automatic screen shot, reload Intel Map after screen shot is captured always.

#### Capture interval min.
Automatic screen shot capturing interval minutets.

#### Web view scaling ratio
Web is scaled and shown on the overlay. 1.0 means same scale in browser.

### WEB SETTING
#### Base load URL
Load target URL. Defaultly, no-option Intel Map is loaded.</br>
You can use Intel Map URL with position/zoom or link simulation.

## LIMITATION
 - Overlay Intel Map is available only on portrait configuration.

## NOTICE
If Intel Map loading is very slow, screen shot can not include all components (portal / link).
