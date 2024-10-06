# Android Code Challenge
A custom gallery app that displays both images and videos from your device, with options to filter by media type. Users can view all images, all videos, or explore specific folders using a dedicated folder list for easy navigation.

---
# Built With
1. Kotlin - Primary programming language.
2. Jetpack Navigation Component - For handling navigation.
3. MVVM architecture - For architecture approach that divides UI & logic code.
4. Glide - For image loading and caching.
5. Koin - For dependency injection.
6. Coroutines and Flow - For reactive programing.
7. Veiw binding - For Type-Safe access to views.
8. Git versioning - Add relevant commits.
9. Custom created caching system - For high performance with smooth scrolling.

---
# How implemented caching system.
The system temporarily stops video thumbnail generation during active scrolling to prevent performance lags.
Once scrolling stops, the system identifies the first and last visible item positions in the RecyclerView.
It then initiates thumbnail generation for items within this range, adding them to a processing queue. 
If a user quickly resumes scrolling before the thumbnail generation is complete, the system continues to generate and queue thumbnails for the items currently in view.
Using this system we can achieve very smooth scrolling media list.
