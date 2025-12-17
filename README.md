# MyGarden

<div align="center">


**A smart plant care companion that helps you keep your plants healthy and thriving**

</div>

---
## About

**MyGarden** is a mobile application designed to help plant enthusiasts maintain healthy plants by
providing intelligent reminders, plant health tracking, and community features. Whether you're a
seasoned gardener or just starting out, MyGarden makes plant care effortless and rewarding.

### The Problem

Many people enjoy having plants but struggle with:

- Forgetting watering schedules
- Not tracking plant health over time
- Not knowing when plants are overwatered or underwatered
- Difficulty identifying plants and their care requirements

### The Solution

MyGarden offers:

- **Smart Reminders**: Never forget to water your plants again
- **Health Tracking**: Visual indicators showing plant health status
- **Photo Documentation**: Track growth and identify issues with your phone's camera
- **Social Features**: Connect with friends, share gardens, and compete on leaderboards
- **Achievements**: Earn points for consistent care and healthy plants
- **Offline Mode**: Full functionality even without internet connection

---
## Main Sensor: Camera

The camera is the **primary sensor** of MyGarden, serving as your plant care companion's eyes.
- **Plant Identification**: Capture a photo to instantly identify plant species using the PlantNet
  API

---
## Features

### Core Functionality

- **Plant Management**
    - Add plants with custom names, species, and watering schedules
    - Upload photos to track growth and health over time
    - Edit and delete plants from your garden
    - View detailed plant information and care history

- **Smart Watering System**
    - Automatic health status calculation based on watering frequency
    - Visual health indicators (Healthy, Slightly Dry, Needs Water, Severely Dry, etc.)
    - Push notifications when plants need attention
    - Water logging with timestamp tracking

- **Camera Integration**
    - Capture high-quality plant photos
    - Plant identification using PlantNet API
    - Photo history and growth tracking
    - Automatic EXIF data preservation

- **Social Features**
    - Add friends and view their gardens
    - Send and receive friend requests
    - Leaderboard with points for plant care
    - Activity feed showing friends' plant activities

- **Achievements System**
    - Earn achievements for various milestones
    - Track healthy plant streaks
    - Compete with friends on the leaderboard
    - Unlock badges for consistent care

- **Offline Mode**
    - Full app functionality without internet
    - Local data caching with Firebase persistence
    - Automatic sync when connection restored
    - Offline indicator with smart UI updates

### Advanced Features

- **Push Notifications**
    - Water reminders based on plant health
    - Friend request notifications
    - Achievement unlocked alerts
    - Customizable notification preferences

- **AI-Powered Features**
    - Plant care tips using Firebase Gemini AI
    - Plant species identification
    - Health status predictions
    - Personalized care recommendations

---

## Architecture

MyGarden follows a clean architecture pattern with clear separation between UI, business logic, and
data layers:

<div align="center">
<img width="1375" alt="Architecture Diagram M3" src="https://github.com/user-attachments/assets/fb543d87-41f6-4a11-a118-abf5abec2372" />
</div>

---
## Getting Started

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/MyGarden23/MyGarden.git
   cd MyGarden
   ```

2. **Open in Android Studio**
    - Launch Android Studio
    - Select "Open an Existing Project"
    - Navigate to the cloned directory

3. **Configure API Keys**

   Create a `local.properties` file in the project root:
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   PLANTNET_API_KEY=your_plantnet_api_key_here
   ```

   Get a free PlantNet API key from: https://my.plantnet.org/

4. **Setup Firebase**

   See [Firebase Setup](#-firebase-setup) section below for detailed instructions.

5. **Sync Gradle**
   ```bash
   ./gradlew build
   ```

6. **Run the app**
    - Connect an Android device or start an emulator
    - Click "Run" in Android Studio
    - Or use command line:
      ```bash
      ./gradlew installDebug
      ```




---

## How It Works

MyGarden uses a simple yet effective workflow to help you care for your plants:

1. **Add Your Plants** - Use the camera to identify plant species or manually enter details with
   custom watering schedules
2. **Track Health** - The app automatically calculates plant health based on your watering history
   and sends reminders when needed
3. **Stay Connected** - Share your garden with friends, earn achievements, and compete on the
   leaderboard
4. **Get Smart Tips** - Receive AI-powered care recommendations tailored to your specific plants

---


## Figma and Wiki

- **Figma Design**: [here](https://www.figma.com/design/3iAjAd0sxYwH84R5g7eaNu/MyGarden?node-id=522-166&p=f&t=qpY3u1nGwKuZhnQu-0)
- **Wiki**: [here](https://github.com/MyGarden23/MyGarden/wiki)

---




## Team

<div align="center">

| Name             | Role                 |
|------------------|----------------------|
| Matteo Ossipow   | Developer |
| Matteo Meyer     | Developer |
| Simon Perraudin  | Developer |
| Ulysse Du Sordet | Developer |
| Fabio Conti      | Developer |
| Adrien Huot      | Developer |
| Loann Colli      | Developer |

</div>

---

<div align="center">

**Made with ❤️ and 🌱 by the MyGarden Team**



</div>