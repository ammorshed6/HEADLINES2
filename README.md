# আজকের শিরোনাম – Android প্রজেক্ট

Android WebView অ্যাপ। GitHub Actions নিজে APK বানায় এবং Releases-এ প্রকাশ করে।

## ব্যবহার
1. এই ZIP-এর সব ফাইল GitHub রিপোজিটরির মূল ফোল্ডারে আপলোড করুন (`.github/workflows/build.yml`সহ)।
2. `main` ব্রাঞ্চে পুশ হলেই বিল্ড শুরু হবে। Actions ট্যাবে `Build APK` দেখুন।
3. বিল্ড শেষে Releases থেকে `AjkerShironam.apk` নামিয়ে ইনস্টল করুন।

## গুরুত্বপূর্ণ
- খবর আনার কাজ অ্যাপের নেটিভ ব্রিজে (`MainActivity.java`) হয়, তাই Google News-এ CORS সমস্যা নেই।
- `app/release.jks` স্থায়ী সাইনিং কী। এটি মুছবেন না, নইলে পরের আপডেট পুরোনোটির ওপর ইনস্টল হবে না।
- প্রতিটি বিল্ডে `versionCode` বাড়ে (GitHub run number)।
- প্যাকেজ নাম: `com.ammorshed.headlines`। আগের অ্যাপের প্যাকেজ নাম আলাদা হলে প্রথমবার পুরোনো অ্যাপ আনইনস্টল করতে হবে।
- খবরের তালিকা বদলাতে `app/src/main/assets/index.html`-এর `TABS` অংশ দেখুন।
