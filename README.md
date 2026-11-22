<p align="center">
  <img src="assets/banner.png" alt="FeNyx — Simple, private file sharing" width="100%">
</p>

<h1 align="center">FeNyx</h1>

<p align="center">
  <em>Send files from one device to another. Simple and private.</em>
</p>

<p align="center"><a href="https://github.com/Dray-Ashcroft/Crovan/releases"><strong>Download the app</strong></a></p>

<p align="center">
  <a href="#about">About</a> ·
  <a href="#screenshots">Screenshots</a> ·
  <a href="#two-modes">Modes</a> ·
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

FeNyx lets you send files, folders or text from one phone, tablet or computer to another. The sender gets a short code or a QR code; the receiver types the code or scans the QR, and the file arrives. You don't need an account, a cable or the same Wi-Fi, and only the two devices can see what is sent.

FeNyx is built on a free tool called [croc](https://github.com/schollz/croc) and on the Android app made by [Dking08](https://github.com/Dking08). It was earlier called Kroc-Sauria and then Crovan.

---

## Screenshots

<table width="100%">
  <tr>
    <td width="50%" align="center">
      <img src="Screenshots/Jarvis.png" alt="Jarvis mode on a phone" width="85%">
      <br><sub><b>1 · Jarvis (Phone)</b></sub>
    </td>
    <td width="50%" align="center">
      <img src="Screenshots/Friday.png" alt="Friday mode showing a large QR code" width="85%">
      <br><sub><b>2 · Friday (Panel)</b></sub>
    </td>
  </tr>
  <tr>
    <td width="50%" align="center">
      <img src="Screenshots/Transmit.png" alt="Transmit screen for sending" width="85%">
      <br><sub><b>3 · Transmit (Send)</b></sub>
    </td>
    <td width="50%" align="center">
      <img src="Screenshots/Retrieve.png" alt="Retrieve screen for receiving" width="85%">
      <br><sub><b>4 · Retrieve (Receive)</b></sub>
    </td>
  </tr>
</table>

---

## Two Modes

The app has two modes. The small light next to the title shows which one you are in.

| Mode | Light colour | Use it on | What it does |
|:--|:--|:--|:--|
| **Jarvis** | Blue | Your phone | Send (**Transmit**) and receive (**Retrieve**). Tap the round buttons at the bottom or swipe to switch. |
| **Friday** | Orange | A tablet or classroom smart board | Only receives. It shows a big QR code so anyone can scan it with their phone and send a file. |

**To change mode:** press and hold the light for one second.
**Friday's QR code** changes by itself every 10 minutes and after each file, so the code on screen always works.
**To send to a smart board:** on your phone, open Transmit, tap **Scan panel QR**, then choose the file.

---

## What It Can Do

| Feature | What it means |
|:--|:--|
| **Send** | Send files, whole folders, or a piece of text |
| **Receive** | Scan a QR code or type a code; files are saved in your `Downloads/FeNyx` folder |
| **Progress ring** | A ring fills up and changes from red to yellow to green as the file moves, with speed and time left |
| **Big files** | Large files (10 GB and more) send without the app closing |
| **Send now, collect later** | Upload a locked copy that the other person can download later from a link; delete it any time |
| **History** | See what you sent and received, and mark favourites |
| **Share button** | Send straight from your gallery or file manager using Android's Share button |
| **Finish animation** | A short animation plays when a transfer finishes. Tap to skip, or turn it off in Settings. |
| **Languages** | English, Español, हिन्दी, 简体中文 and 繁體中文 |

---

## Which Version to Download

| Version | Size | Choose it if |
|:--|:--|:--|
| **Standard** | about 16 MB | You send to people who may have an older app. It works with old and new versions. |
| **Lite** | about 12 MB | Everyone has an up-to-date app and you want a smaller download. |

Both look and work the same. You can install one over the other.

---

## Install

1. Download the app file (APK) from [Releases](https://github.com/Dray-Ashcroft/Crovan/releases).
2. Open it on your phone and tap **Install**. If asked, allow installing apps from this source.

**Works on:** Android 8 or newer, on most phones made in the last few years. It does not work on very old or 32-bit phones.

**Already have Crovan?** FeNyx installs as a new app next to it. You can uninstall Crovan once you no longer need its history.

<details>
<summary><b>Why the app asks for permissions</b></summary>

| Permission | Why |
|:--|:--|
| Camera | To scan QR codes |
| Notifications | To show progress and keep sending when the screen is off |
| Network | To send and receive files |
| Files and photos | To pick files to send and save files you receive |

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
| Finish animation | [Iron Man stealth suit (Textured)(Rigged)](https://skfb.ly/oGIAu) | 3D MODELS | CC BY 4.0 |
| Mode-change rings | HUD | anuhya | CC BY 4.0 |

The models were posed, darkened and turned into images for the app. See [ASSET_CREDITS.md](ASSET_CREDITS.md).

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
| RC V | Ember | 22 Nov 2026 |
| RC IV | Delta | 27 Oct 2026 |
| RC III | Triton | 10 Oct 2026 |
| RC II | Requiem | 1 Oct 2026 |
| RC I | Dawn | 1 Oct 2026 |

---

## License

FeNyx is free and open source under the **GNU GPL v3.0** (see [LICENSE](LICENSE)). The croc code in `third_party/` uses the **MIT** licence.

<p align="center">
  <sub>Built on the Android croc app · Powered by croc</sub><br>
  <sub>GitHub · Codeberg</sub>
</p>
