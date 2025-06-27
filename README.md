# Local Food Finder 

## Key Features

### For Customers 
- **Interactive Map**: Real-time map showing nearby food vendors with location markers
- **QR Code Scanner**: Scan vendor QR codes to instantly view location or vendor details
- **Vendor Discovery**: Browse and search for local food vendors in your area
- **Vendor Details**: View comprehensive information about each vendor including menus, hours, and contact info
- **Location-Based Search**: Find vendors within a customizable radius
- **User Authentication**: Secure login and registration system

### For Vendors 
- **Business Profile Setup**: Complete vendor registration with business details
- **Location Management**: Set and update business location with GPS integration
- **Menu Upload**: Upload and manage menu photos with Firebase Storage
- **QR Code Generation**: Generate custom QR codes for easy customer access
- **Business Hours**: Set operating hours and business information
- **Dietary Options**: Mark food as vegan, halal, or gluten-free

##  Technical Stack

### Frontend
- **Platform**: Android (Java)
- **Minimum SDK**: API 31 (Android 12)
- **Target SDK**: API 35
- **UI Framework**: Android Views with Material Design

### Backend & Services
- **Authentication**: Firebase Authentication
- **Database**: Firebase Realtime Database
- **Storage**: Firebase Storage (for menu photos)
- **Maps**: Google Maps Android API
- **Location**: Google Play Services Location API

### Key Libraries
- **QR Code**: ZXing Android Embedded (4.3.0)
- **Image Loading**: Glide (4.16.0)
- **Maps**: Google Play Services Maps (18.2.0)
- **Location**: Google Play Services Location (21.1.0)
- **Firebase**: Auth (22.3.0), Database (20.3.0), Storage (20.3.0)

##  App Architecture

### User Roles
1. **Customer**: Browse vendors, scan QR codes, view maps
2. **Vendor**: Manage business profile, upload menus, generate QR codes

### Core Activities
- `LoginActivity`: User authentication and login
- `RegisterActivity`: New user registration with role selection
- `MainActivity`: Main dashboard with role-based UI
- `VendorSetupActivity`: Vendor profile creation and management
- `QRScannerActivity`: QR code scanning functionality
- `VendorQRCodeActivity`: QR code generation for vendors
- `MenuUploadActivity`: Menu photo upload and management
- `VendorDetailActivity`: Detailed vendor information display
- `MapFragment`: Interactive Google Maps integration


## Database Structure

### Users Collection
```json
{
  "users": {
    "userId": {
      "name": "string",
      "email": "string",
      "role": "customer|vendor"
    }
  }
}
```

### Vendors Collection
```json
{
  "vendors": {
    "vendorId": {
      "businessName": "string",
      "description": "string",
      "phone": "string",
      "latitude": "double",
      "longitude": "double",
      "openTime": "string",
      "closeTime": "string",
      "menuPhotoUrl": "string",
      "approved": "boolean",
      "ratingAvg": "double",
      "ratingCount": "integer",
      "isVegan": "boolean",
      "isHalal": "boolean",
      "isGlutenFree": "boolean"
    }
  }
}
```

##  QR Code System

### QR Code Formats
1. **Google Maps URL**: `https://maps.google.com/maps?q=LAT,LNG(BusinessName)`
2. **App-Specific URI**: `localfoodfinder://vendor?id=VENDOR_ID`

### Scanning Flow
1. Customer taps "Scan QR" button
2. Camera opens with ZXing scanner
3. QR code is processed and appropriate action is taken:
   - Google Maps URLs open in Maps app
   - App-specific URIs open vendor details

##  Features in Detail

### Location Services
- Real-time GPS location tracking
- Vendor proximity detection
- Interactive map with custom markers
- Location-based vendor filtering

### Image Management
- Menu photo upload with compression
- Firebase Storage integration
- Glide-powered image loading
- File provider for secure image sharing

### User Experience
- Role-based UI adaptation
- Intuitive navigation
- Real-time data synchronization
- Offline-friendly design patterns

### Customer Journey
1. Register/Login → Main Dashboard → View Map → Scan QR Codes → Discover Vendors

### Vendor Journey
1. Register as Vendor → Setup Business Profile → Upload Menu → Generate QR Code → Manage Location

##  Supported Android Versions
- **Minimum**: Android 12 (API 31)
- **Target**: Android 14 (API 35)
- **Recommended**: Android 12+ for optimal performance

