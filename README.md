<p align="center">
  <img src="assets/banner.png" alt="F3Nyx — Simple, private file sharing" width="100%">
</p>

<h1 align="center">F3Nyx (Phoenix)</h1>

<p align="center">
  <em>Send files from one device to another. Simple and private.</em>
</p>

<p align="center">
  <a href="https://github.com/Dray-Ashcroft/F3Nyx/releases/latest"><img src="https://img.shields.io/github/v/release/Dray-Ashcroft/F3Nyx?style=for-the-badge&label=Release&color=E5484D&labelColor=0E0E10" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-E2B33C?style=for-the-badge&labelColor=0E0E10&logo=android&logoColor=white" alt="Android 8.0 or newer">
  <img src="https://img.shields.io/badge/Encryption-End--to--end-38BDF8?style=for-the-badge&labelColor=0E0E10" alt="End-to-end encrypted">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-A0A0A8?style=for-the-badge&labelColor=0E0E10" alt="GPL-3.0 licence"></a>
</p>

<p align="center">
  <a href="https://github.com/Dray-Ashcroft/F3Nyx/releases/latest"><strong>Download the app</strong></a>
</p>

<p align="center">
  <a href="#about">About</a> ·
  <a href="#how-it-works">How it works</a> ·
  <a href="#screenshots">Screenshots</a> ·
  <a href="#two-layouts">Layouts</a> ·
  <a href="#what-it-can-do">Features</a> ·
  <a href="#which-version-to-download">Versions</a> ·
  <a href="#install">Install</a> ·
  <a href="#credits">Credits</a> ·
  <a href="#releases">Releases</a> ·
  <a href="#license">License</a>
</p>

---

<p align="center"><em>Project dedicated to Lord Vishnu</em></p>

---

## About

F3Nyx lets you send files, folders or text from one phone, tablet or computer to another. The sender gets a short code or a QR code; the receiver types the code or scans the QR, and the file arrives. You don't need an account, a cable or the same Wi-Fi, and only the two devices can see what is sent.

F3Nyx is built on a free tool called [croc](https://github.com/schollz/croc) and on the Android app made by [Dking08](https://github.com/Dking08). It rose from Kroc-Sauria and Crovan, hence the name: a phoenix.

<table width="100%">
  <tr>
    <td width="25%" align="center"><h3>0</h3><sub>accounts needed</sub></td>
    <td width="25%" align="center"><h3>10 GB+</h3><sub>files, without crashing</sub></td>
    <td width="25%" align="center"><h3>2</h3><sub>layouts: phone and smart board</sub></td>
    <td width="25%" align="center"><h3>5</h3><sub>languages</sub></td>
  </tr>
</table>

---

## How It Works

<table width="100%">
  <tr>
    <td width="33%" align="center">
      <h3>01</h3>
      <b>Pick</b><br>
      <sub>Choose files, a folder or text on the sending device</sub>
    </td>
    <td width="33%" align="center">
      <h3>02</h3>
      <b>Share the code</b><br>
      <sub>Show the QR or read out the short code</sub>
    </td>
    <td width="33%" align="center">
      <h3>03</h3>
      <b>Receive</b><br>
      <sub>Scan or type it on the other device. Done.</sub>
    </td>
  </tr>
</table>

---

## Screenshots

<table width="100%">
  <tr>
    <td width="50%" align="center">
      <img src="Screenshots/Phone.png" alt="Phone layout with the Transmit screen" width="85%">
      <br><sub><b>1 · Phone layout</b></sub>
    </td>
    <td width="50%" align="center">
      <img src="Screenshots/Panel.png" alt="Panel layout with a large QR code" width="85%">
      <br><sub><b>2 · Panel layout</b></sub>
    </td>
  </tr>
  <tr>
    <td width="50%" align="center">
      <img src="Screenshots/Transmit.png" alt="Transmit screen while sending" width="85%">
      <br><sub><b>3 · Transmit (Send)</b></sub>
    </td>
    <td width="50%" align="center">
      <img src="Screenshots/Retrieve.png" alt="Retrieve screen for receiving" width="85%">
      <br><sub><b>4 · Retrieve (Receive)</b></sub>
    </td>
  </tr>
</table>

---

## Two Layouts

A small switch in the centre of the top bar, with a phone icon and a panel icon, shows which layout you are in. Tap the other icon to change it.

<table width="100%">
  <tr>
    <td width="50%" valign="top">
      <h3>Phone</h3>
      <sub>FOR YOUR PHONE</sub>
      <p>Send (<b>Transmit</b>) and receive (<b>Retrieve</b>). Tap the round buttons at the bottom or swipe to switch. Pick files, a folder or text with three small icons.</p>
    </td>
    <td width="50%" valign="top">
      <h3>Panel</h3>
      <sub>FOR A TABLET OR CLASSROOM SMART BOARD</sub>
      <p>Only receives. A big QR code waits on screen; anyone scans it with F3Nyx and sends. Files arrive in a <b>Received today</b> list.</p>
    </td>
  </tr>
</table>

**Back-to-back sending:** the panel keeps the same code for 10 minutes and is ready again within seconds after each file. Your phone remembers the panel, so you can send file after file without scanning again.
**To send to a smart board:** on your phone, open Transmit, tap **Scan panel QR**, then choose the file.

<details>
<summary><b>Panel buttons</b></summary>

| Button | What it does |
|:--|:--|
| Big QR | Shows a full-screen QR with the code in large letters, for the back of a classroom |
| New code | Makes a fresh code at once |
| Animation | Turns the finish animation on or off (off by default on panels) |
| i | Shows or hides the three steps for sending to the board |

</details>

---

## What It Can Do

<table width="100%">
  <tr>
    <td width="33%" valign="top"><b>Send anything</b><br><sub>Files, whole folders or a piece of text</sub></td>
    <td width="33%" valign="top"><b>Receive by QR or code</b><br><sub>Saved in your <code>Downloads/F3Nyx</code> folder</sub></td>
    <td width="33%" valign="top"><b>Progress ring</b><br><sub>Fills red → yellow → green, with speed and time left</sub></td>
  </tr>
  <tr>
    <td valign="top"><b>Big files</b><br><sub>10 GB and more, without the app closing</sub></td>
    <td valign="top"><b>Notifications</b><br><sub>Progress, Cancel, then Open or Share when files arrive</sub></td>
    <td valign="top"><b>Share all</b><br><sub>Share every received file at once</sub></td>
  </tr>
  <tr>
    <td valign="top"><b>Send now, collect later</b><br><sub>A locked copy the other person downloads later from a link</sub></td>
    <td valign="top"><b>History</b><br><sub>Everything sent and received, with favourites</sub></td>
    <td valign="top"><b>Share button</b><br><sub>Send straight from your gallery or file manager</sub></td>
  </tr>
  <tr>
    <td valign="top"><b>Finish animation</b><br><sub>A short lift-off or landing in its own card; tap to skip</sub></td>
    <td valign="top"><b>Themes</b><br><sub>System, Light, Dark and Monet (wallpaper colours)</sub></td>
    <td valign="top"><b>Languages</b><br><sub>English, Español, हिन्दी, 简体中文, 繁體中文</sub></td>
  </tr>
</table>

---

## Which Version to Download

| Version | Size | Choose it if |
|:--|:--|:--|
| **Standard** | about 16 MB | You send to people who may have an older app. It works with old and new versions. Best for smart boards. |
| **Lite** | about 12 MB | Everyone has an up-to-date app and you want a smaller download. |

Both look and work the same. You can install one over the other.

---

## Install

1. Download the app file (APK) from [Releases](https://github.com/Dray-Ashcroft/F3Nyx/releases/latest).
2. Open it on your phone and tap **Install**. If asked, allow installing apps from this source.
3. On Android 13 and newer, allow notifications when asked, to see transfer progress.

**Works on:** Android 8 or newer, on most phones made in the last few years. It does not work on very old or 32-bit phones.

**Already have Crovan?** F3Nyx installs as a new app next to it. You can uninstall Crovan once you no longer need its history.

<details>
<summary><b>Why the app asks for permissions</b></summary>

| Permission | Why |
|:--|:--|
| Camera | To scan QR codes |
| Notifications | To show progress and finished transfers |
| Network | To send and receive files |
| Files and photos | To pick files to send and save files you receive |

</details>

<details>
<summary><b>Check your download</b></summary>

Each release has a `SHA256SUMS.txt`. On a computer, compare it with:

```bash
sha256sum F3Nyx-standard-release.apk
```

</details>

---

## For Developers

<details>
<summary><b>Building the app yourself</b></summary>

You need Java 17, Go (version listed in `third_party/croc-src/go.mod`) and Gradle 8.9. Then run:

```bash
gradle assembleStandardRelease assembleLiteRelease
```

- The croc source code is kept in `third_party/`: the current version (11.5.2) and an older one (10.6.0) used only in Standard.
- A test build is made automatically on every update to `master`. Release builds are started by hand from the Actions tab.
- A weekly check updates croc when a new version comes out.

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before contributing. Translations are done on Crowdin.

</details>

---

## Credits

| Contributor | Role |
|:--|:--|
| Sai Prasad, M.Sc. Agri. Entomology, ASRB NET | Project Designer |
| Claude Opus 5.5 | Development |
| OpenAI Astra 6 | Image Generation |
| Dastageer Siddiqui ([Dking08](https://github.com/Dking08)) | Original Android app |
| [schollz](https://github.com/schollz/croc) | croc, the tool that does the sending |

<details>
<summary><b>3D models</b></summary>

| Used for | Model | Creator | Licence |
|:--|:--|:--|:--|
| Finish animation | [Iron-Man Mark 85 \| Rigged](https://sketchfab.com/3d-models/iron-man-mark-85-rigged-dde1085c464d4f8da259fe6669ae4dd2) | 9A Films / Nihar Arora | CC BY 4.0 |
| Switch-screen rings | HUD | anuhya | CC BY 4.0 |

The models were posed and turned into images for the app. See [ASSET_CREDITS.md](ASSET_CREDITS.md).

</details>

<details>
<summary><b>Other software and fonts</b></summary>

| Component | Licence |
|:--|:--|
| croc and the tools it uses | MIT and BSD 3-Clause |
| Inter and JetBrains Mono fonts | SIL OFL 1.1 |
| Android libraries and ZXing (QR scanner) | Apache 2.0 |

</details>

---

## Releases

| Release | Title | Date |
|:--|:--|:--|
| RC V | Ember | 23 Nov 2026 |
| RC IV | Delta | 27 Oct 2026 |
| RC III | Triton | 10 Oct 2026 |
| RC II | Requiem | 1 Oct 2026 |
| RC I | Dawn | 1 Oct 2026 |

---

## License

F3Nyx is free and open source under the **GNU GPL v3.0** (see [LICENSE](LICENSE)). The croc code in `third_party/` uses the **MIT** licence.

<p align="center">
  <sub>Built on the Android croc app · Powered by croc</sub><br>
  <sub>GitHub · Codeberg</sub>
</p>
