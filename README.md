# District 49 Child and Youth Care Centre - Mobile Application

A comprehensive Android application built for District 49 Child and Youth Care Centre in Umkomaas, KwaZulu-Natal, South Africa. The app facilitates donations, volunteer engagement, event management, and community connection while providing administrative tools for the organization.

---

## Contents

* Project Overview
* Features
* Tech Stack
* Getting Started
* Firebase Setup
* Related Repositories
* User Flow
* Team Roles and Contributions
* Admin Access
* Testing and Deployment
* FAQs
* Credits
* Links
* References

---

## Project Overview

District 49 Child and Youth Care Centre houses approximately 26-30 children from diverse backgrounds in Umkomaas, KwaZulu-Natal. This mobile application serves as a digital bridge connecting the organization with donors, volunteers, and the community.

### Mission
To provide a safe, caring environment for vulnerable children while facilitating community engagement and support through modern technology.

### Key Objectives
* Streamline donation management and tracking
* Facilitate event coordination and community engagement
* Share success stories and organizational updates
* Enable real-time communication through push notifications
* Provide administrative tools for content management

---

## Features

### User Features

* **Secure Authentication**
  * Email and password login/registration
  * Password reset functionality
  * Role-based access control (User/Admin)
  * Profile management with photo uploads

* **Donation System**
  * Real-time donation tracking with visual progress
  * Monetary donations via PayFast payment gateway
  * Donation goal visualization with charts
  * Physical donation information
  * Secure payment processing

* **Events Management**
  * Interactive drag-and-drop events calendar
  * Customizable event tiles with colors
  * Event details with date, location, description
  * Real-time updates

* **Success Stories**
  * Swipeable story cards
  * Pull-up-to-expand functionality
  * Image support for authors
  * Anonymous story option

* **Gallery**
  * Photo grid display
  * Fullscreen image viewer
  * Swipe navigation
  * Upload capabilities

* **Newsletter System**
  * Email subscription
  * PDF newsletter distribution
  * Subscriber management

* **Push Notifications**
  * Event notifications
  * News and announcement alerts
  * Newsletter availability notifications
  * Customizable preferences

* **Additional Features**
  * News and announcements feed
  * Contact form
  * Meet the Team section
  * Our Story page
  * Multi-language support

### Administrative Features

* **Admin Dashboard**
  * Donation goal management
  * News publishing
  * Newsletter distribution
  * Event creation
  * Success story moderation
  * Team member management
  * Push notification broadcasting

* **Content Management**
  * Create, edit, delete content
  * Upload and organize media
  * Moderate user submissions
  * Analytics and reporting

---

## Tech Stack

### Frontend
* **Kotlin** - Primary programming language
* **Jetpack Compose** - Modern declarative UI
* **XML Layouts** - Traditional Android UI
* **Material Design 3** - UI components
* **Coil & Glide** - Image loading and processing
* **MPAndroidChart** - Data visualization

### Backend Services
* **Firebase Authentication** - User management
* **Firebase Firestore** - Real-time database
* **Firebase Storage** - File storage
* **Firebase Cloud Messaging** - Push notifications
* **Firebase Cloud Functions** - Serverless backend
* **Firebase App Check** - Security

### Payment & Email
* **PayFast API** - Payment gateway
* **Custom API Server** - Hosted on Render
* **Google Apps Script** - Email delivery

### Development Tools
* **Android Studio** - IDE
* **Gradle** - Build automation
* **Git/GitHub** - Version control
* **Figma** - UI/UX design

---

## Getting Started

### Prerequisites

* Android Studio Arctic Fox or later
* JDK 11 or higher
* Android SDK 24 or higher
* Firebase project
* PayFast merchant account (for payments)

### Installation

#### Clone the repository
```bash
git clone https://github.com/ST10145498/District49_Android.git
cd District49_Android
```

#### Open in Android Studio

1. Launch Android Studio
2. Select File → Open
3. Navigate to the cloned repository
4. Click OK

#### Configure Firebase

1. Download google-services.json from Firebase Console
2. Place it in the app/ directory
3. Sync project with Gradle files

#### Build and Run
```bash
./gradlew build
./gradlew installDebug
```

Or use Android Studio Run button (Shift+F10)

---

## Firebase Setup

### Create Firebase Project

1. Go to https://console.firebase.google.com
2. Click "Add project"
3. Follow setup wizard
4. Add Android app with package name: `vcmsa.projects.district49_android`

### Enable Required Services

* **Authentication**: Enable Email/Password provider
* **Firestore Database**: Create in production mode
* **Storage**: Create default bucket
* **Cloud Messaging**: Enable FCM
* **App Check**: Configure Play Integrity

### Firestore Collections

The app uses the following collections:
* `users` - User profiles and roles
* `donation_goals` - Donation tracking
* `newsletter_subscribers` - Newsletter subscriptions
* `newsletters` - Sent newsletters
* `news_announcements` - News posts
* `eventsConfig` - Event calendar data
* `success_stories` - Success stories
* `gallery` - Gallery images
* `meet_the_team` - Team members
* `user_notification_preferences` - Notification settings
* `user_fcm_tokens` - Push notification tokens

### Configure Security Rules

Set up Firestore and Storage security rules to ensure:
* Users can only edit their own profiles
* Admin-only write access for content management
* Read access for public content
* Authenticated access for private content

---

## Related Repositories

### Firebase Cloud Functions
Push notification system hosted on Firebase:
```
https://github.com/ST10145498/Districy49-Firebase-Notification-Function.git
```

**Purpose**: Serverless functions for sending push notifications to users based on their preferences

**Key Features**:
* Send notifications to all users with specific preferences
* Filter by notification type (events, news, newsletters)
* Track success/failure counts
* Skip users with disabled preferences

### PayFast API Server
Custom payment processing server hosted on Render:
```
https://github.com/ST10145498/PayFastAPI.git
```

**Purpose**: Secure payment initiation and callback handling

**Key Features**:
* Payment initiation with PayFast
* Secure signature generation
* Return URL handling
* Payment status verification

### Main Application Repository
```
https://github.com/ST10145498/District49_Android.git
```

---

## User Flow

### First-Time User Journey

1. **Welcome Screen**
   * Animated logo display
   * Swipe up to continue

2. **Login/Register Selection**
   * Choose to login or create account

3. **Registration** (if new user)
   * Enter name, surname, email, password
   * Account created with default "user" role
   * Notification preferences initialized

4. **Homepage**
   * View donation progress
   * See latest news
   * Access newsletter subscription
   * Navigate to different sections

5. **Explore Features**
   * Browse events calendar
   * Read success stories
   * View photo gallery
   * Make donations
   * Contact organization

### Donation Flow

1. User navigates to Donate section
2. Views current donation goal and progress
3. Selects donation amount or enters custom amount
4. Reviews terms and conditions
5. Redirected to PayFast payment gateway
6. Completes payment
7. Returns to app with confirmation
8. Donation reflected in real-time progress

### Admin Content Management Flow

1. Admin logs in with admin credentials
2. Accesses Admin Dashboard
3. Selects content type to manage:
   * Update donation goals
   * Publish news announcements
   * Send newsletters
   * Create events
   * Moderate stories
4. Content immediately visible to users
5. Push notifications sent automatically

---


## Admin Access

To access administrative features:

### Admin Login
```
Email: [Contact District 49 for admin credentials]
Password: [Contact District 49 for admin credentials]
```

### Admin Capabilities

Once logged in as admin, you can:
* Access Admin Dashboard from Homepage (long-press menu button)
* Update donation goals
* Publish news announcements
* Send newsletters with PDF attachments
* Create and manage events
* Moderate success stories
* Manage gallery content
* Add/remove team members
* Send test notifications
* View analytics

### Testing Admin Features

For development and testing purposes, you can:
1. Create a test admin account in Firebase Console
2. Set the `userRole` field to "admin" in Firestore
3. Login with the test account
4. Access all admin features

---

## Testing and Deployment

### Testing Performed

* **Authentication Testing**
  * Login/logout functionality
  * Registration with validation
  * Password reset flow
  * Role-based access control

* **Feature Testing**
  * Donation flow end-to-end
  * Payment gateway integration
  * Event creation and display
  * Success story submission
  * Gallery upload and viewing
  * Newsletter subscription
  * Push notifications
  * Contact form submission

* **UI Testing**
  * Responsive layouts
  * Navigation flow
  * Form validation
  * Error handling
  * Loading states

* **Device Testing**
  * Multiple Android versions (API 24+)
  * Different screen sizes
  * Orientation changes
  * Network conditions

### Deployment Process

#### Generate Signed APK/AAB

1. Build → Generate Signed Bundle/APK
2. Select Android App Bundle
3. Create or use existing keystore
4. Select release build variant
5. Build APK/AAB

#### Play Store Preparation

1. Create app listing
2. Add screenshots and descriptions
3. Set content rating
4. Configure pricing and distribution
5. Upload release bundle
6. Submit for review

#### Internal Testing

1. Create internal testing track
2. Add test users
3. Upload AAB
4. Share test link
5. Gather feedback

---

## FAQs

**Q1: How do I reset my password?**

Click "Forgot Password?" on the login screen, enter your email, and follow the reset link sent to your email.

**Q2: Why can't I see admin features?**

Admin features are only visible to users with the "admin" role. Contact District 49 to request admin access if needed.

**Q3: How do I subscribe to the newsletter?**

A popup will appear on the Homepage prompting you to subscribe. You can also manage your subscription through notification settings.

**Q4: Are my donations secure?**

Yes, all donations are processed through PayFast, a certified Payment Service Provider. We do not store your payment information.

**Q5: How do I upload a success story?**

Only administrators can upload success stories to ensure content quality and appropriateness. Contact District 49 to submit your story.

**Q6: Why am I not receiving notifications?**

Check your notification preferences in the Notification Settings page. Also ensure notifications are enabled in your device settings.

**Q7: Can I donate physical items?**

Yes! View the "Donations We Accept" page to see what items we accept and how to arrange drop-offs.

**Q8: How do I edit my profile?**

Navigate to the Profile page, tap "Edit Profile", make your changes, and tap "Save".

**Q9: What events are coming up?**

View the Events page to see an interactive calendar of upcoming events with dates, locations, and descriptions.

**Q10: How can I contact District 49?**

Use the Contact Us form in the app, or visit the Our Story page for more contact information.

---

## Credits

### Development Team

* **Kyle Jeremiah Govender** (ST10145498) - Project Manager, Developer
* **Deshalin Naicker** - Frontend Developer
* **Jadzia Naidoo** - Developer
* **Nivad Ramdass** - Frontend Developer
* **Siyabonga Mfusi** - Backend Developer, Database Specialist
* **Preeyunka Moodley** - Documentation Lead

### Organization

* **District 49 Child and Youth Care Centre**
* Location: Umkomaas, KwaZulu-Natal, South Africa

### Contact

* **Email**: ST10145498@vcconnect.edu.za
* **Institution**: Varsity College

---

## Links

### Repositories

**Main Application:**
```
https://github.com/ST10145498/District49_Android.git
```

**Firebase Cloud Functions:**
```
https://github.com/ST10145498/Districy49-Firebase-Notification-Function.git
```

**PayFast API Server:**
```
https://github.com/ST10145498/PayFastAPI.git
```


## References

* **Firebase Documentation** - Authentication, Firestore, Storage, Cloud Messaging
  * https://firebase.google.com/docs

* **Android Developers Guide** - Jetpack Compose, Kotlin, Material Design
  * https://developer.android.com

* **PayFast API Documentation** - Payment integration
  * https://developers.payfast.co.za

* **Kotlin Coroutines Guide** - Asynchronous programming
  * https://kotlinlang.org/docs/coroutines-guide.html

* **Material Design 3** - UI/UX guidelines
  * https://m3.material.io

* **Firebase Cloud Functions** - Serverless backend
  * https://firebase.google.com/docs/functions

* **OkHttp Documentation** - HTTP client
  * https://square.github.io/okhttp

* **Glide Image Loading** - Image processing
  * https://github.com/bumptech/glide

* **MPAndroidChart** - Data visualization
  * https://github.com/PhilJay/MPAndroidChart

* **Google Apps Script** - Email automation
  * https://developers.google.com/apps-script


* **ChatGPT** - Code assistance and documentation

---

## License

This project was developed as part of an academic assignment for Varsity College. All rights reserved by the development team and District 49 Child and Youth Care Centre.

---

## Acknowledgments

Special thanks to:
* District 49 Child and Youth Care Centre for the opportunity


---

**Note**: This application is actively maintained and updated. For the latest version and updates, please refer to the GitHub repository.

**Last Updated**: November 2025
