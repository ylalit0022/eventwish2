# Image Capture & Crop Feature - Implementation Scratchpad

## Phase 1: Project Setup & Basic Structure

### Step 1: Create Directory Structure
- [x] Create the base directory structure for the feature:
  ```
  app/src/main/java/com/ds/eventwish/imagecapture/
  ├── fragments/
  ├── adapters/
  ├── viewmodels/
  └── utils/
  ```

### Step 2: Create Initial Resource Files
- [ ] Create layout files:
  - [ ] `app/src/main/res/layout/activity_image_capture.xml`
  - [ ] `app/src/main/res/layout/fragment_camera.xml`
  - [ ] `app/src/main/res/layout/fragment_crop.xml`
  - [ ] `app/src/main/res/layout/fragment_gallery.xml`
  - [ ] `app/src/main/res/layout/item_captured_image.xml`

- [ ] Create drawable resources:
  - [ ] `app/src/main/res/drawable/ic_launcher_camera.xml`
  - [ ] `app/src/main/res/drawable/ic_capture_photo.xml`
  - [ ] `app/src/main/res/drawable/ic_switch_camera.xml`
  - [ ] `app/src/main/res/drawable/ic_crop.xml`
  - [ ] `app/src/main/res/drawable/ic_reorder.xml`
  - [ ] `app/src/main/res/drawable/ic_export.xml`

- [ ] Create value resources:
  - [ ] `app/src/main/res/values/strings_camera.xml`
  - [ ] `app/src/main/res/values/styles_camera.xml`

### Step 3: Update AndroidManifest.xml
- [ ] Add new activity with launcher icon
- [ ] Add required permissions for camera and storage

### Step 4: Add Required Dependencies
- [ ] Update app build.gradle to include:
  - [ ] CameraX dependencies
  - [ ] Image cropping library (such as Android Image Cropper or uCrop)

## Phase 2: Core Implementation

### Step 5: Create ViewModel
- [ ] Implement `ImageCaptureViewModel.java`
  - [ ] Define `CapturedImage` class
  - [ ] Implement LiveData for captured images
  - [ ] Add methods for image management

### Step 6: Implement Main Activity
- [ ] Create `ImageCaptureActivity.java`
  - [ ] Set up ViewModel
  - [ ] Implement permission handling
  - [ ] Set up fragment navigation

### Step 7: Implement Utility Classes
- [ ] Create `BitmapUtils.java`
  - [ ] Implement image combination method
  - [ ] Add bitmap file operations

## Phase 3: Camera Implementation

### Step 8: Implement Camera Fragment
- [ ] Create `CameraFragment.java`
  - [ ] Set up CameraX components
  - [ ] Implement camera preview
  - [ ] Add photo capture functionality

### Step 9: Implement Camera Controls
- [ ] Add UI controls for camera
  - [ ] Capture button
  - [ ] Switch camera button
  - [ ] Gallery navigation button

## Phase 4: Image Cropping

### Step 10: Implement Crop Fragment
- [ ] Create `CropFragment.java`
  - [ ] Set up cropping library
  - [ ] Implement image loading
  - [ ] Add save cropped image functionality

## Phase 5: Gallery Implementation

### Step 11: Create Gallery Adapter
- [ ] Implement `GalleryAdapter.java`
  - [ ] Create ViewHolder class
  - [ ] Implement item click handling
  - [ ] Set up drag and drop support

### Step 12: Implement Gallery Fragment
- [ ] Create `GalleryFragment.java`
  - [ ] Set up RecyclerView with adapter
  - [ ] Add ItemTouchHelper for reordering
  - [ ] Implement export functionality

## Phase 6: Export and Integration

### Step 13: Implement Image Export
- [ ] Add methods to combine images
- [ ] Create export dialog
- [ ] Implement share functionality

### Step 14: Polish and Testing
- [ ] Test permission handling
- [ ] Verify workflow between fragments
- [ ] Test image export and sharing

## Immediate Implementation Tasks

The following tasks should be completed first to establish the foundation of the feature:

1. Create the directory structure
2. Add required dependencies to build.gradle
3. Implement the basic layout files
4. Create the ViewModel and Activity classes
5. Update AndroidManifest.xml with new activity and permissions

### Task 1: Add Required Dependencies

Add the following to app/build.gradle:

```gradle
// CameraX dependencies
implementation "androidx.camera:camera-core:1.2.3"
implementation "androidx.camera:camera-camera2:1.2.3"
implementation "androidx.camera:camera-lifecycle:1.2.3"
implementation "androidx.camera:camera-view:1.2.3"

// Image Cropping library
implementation 'com.github.ArthurHub:Android-Image-Cropper:2.8.0'

// Optional - for animations, etc.
implementation "androidx.activity:activity-ktx:1.6.1"
implementation "androidx.fragment:fragment-ktx:1.5.5"
```

### Task 2: Create Basic Activity Layout

Create the activity_image_capture.xml:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".imagecapture.ImageCaptureActivity">

    <FrameLayout
        android:id="@+id/fragment_container"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toTopOf="@+id/bottom_navigation"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_navigation"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:background="?android:attr/windowBackground"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:menu="@menu/image_capture_navigation" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### Task 3: Create the ImageCaptureViewModel

Implement the basic ViewModel to start:

```java
package com.ds.eventwish.imagecapture.viewmodels;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ImageCaptureViewModel extends ViewModel {
    // LiveData to store captured images
    private final MutableLiveData<List<CapturedImage>> capturedImages = new MutableLiveData<>(new ArrayList<>());
    
    // Store current image being edited/cropped
    private final MutableLiveData<Integer> currentImageIndex = new MutableLiveData<>(-1);
    
    public LiveData<List<CapturedImage>> getCapturedImages() {
        return capturedImages;
    }
    
    public LiveData<Integer> getCurrentImageIndex() {
        return currentImageIndex;
    }
    
    public void setCurrentImageIndex(int index) {
        currentImageIndex.setValue(index);
    }
    
    // Class to represent a captured image
    public static class CapturedImage {
        private Uri originalUri;
        private Uri croppedUri;
        private boolean isCropped = false;
        
        public Uri getOriginalUri() {
            return originalUri;
        }
        
        public void setOriginalUri(Uri originalUri) {
            this.originalUri = originalUri;
        }
        
        public Uri getCroppedUri() {
            return croppedUri;
        }
        
        public void setCroppedUri(Uri croppedUri) {
            this.croppedUri = croppedUri;
            this.isCropped = true;
        }
        
        public boolean isCropped() {
            return isCropped;
        }
        
        public Uri getDisplayUri() {
            return isCropped ? croppedUri : originalUri;
        }
    }
    
    // Methods to add, remove, reorder images
    public void addCapturedImage(Uri imageUri) {
        List<CapturedImage> currentList = capturedImages.getValue();
        CapturedImage newImage = new CapturedImage();
        newImage.setOriginalUri(imageUri);
        currentList.add(newImage);
        capturedImages.setValue(currentList);
    }
    
    public void removeImage(int position) {
        List<CapturedImage> currentList = capturedImages.getValue();
        if (position >= 0 && position < currentList.size()) {
            currentList.remove(position);
            capturedImages.setValue(currentList);
        }
    }
    
    public void reorderImages(int fromPosition, int toPosition) {
        List<CapturedImage> currentList = capturedImages.getValue();
        if (fromPosition < currentList.size() && toPosition < currentList.size()) {
            CapturedImage movedItem = currentList.remove(fromPosition);
            currentList.add(toPosition, movedItem);
            capturedImages.setValue(currentList);
        }
    }
    
    // Methods to update cropped image
    public void setCurrentImageCropped(Uri croppedUri) {
        Integer index = currentImageIndex.getValue();
        if (index != null && index >= 0) {
            List<CapturedImage> currentList = capturedImages.getValue();
            if (index < currentList.size()) {
                CapturedImage image = currentList.get(index);
                image.setCroppedUri(croppedUri);
                capturedImages.setValue(currentList);
            }
        }
    }
}
```

### Task 4: Update AndroidManifest.xml

Add the new activity and permissions to AndroidManifest.xml:

```xml
<!-- Add these permissions above the application tag -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-feature android:name="android.hardware.camera" android:required="true" />

<!-- Add this inside the application tag -->
<activity
    android:name=".imagecapture.ImageCaptureActivity"
    android:exported="true"
    android:theme="@style/Theme.EventWish.ImageCapture">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### Task 5: Create Basic Camera Fragment

Create a simple implementation of the CameraFragment to start:

```java
package com.ds.eventwish.imagecapture.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ds.eventwish.R;
import com.ds.eventwish.imagecapture.viewmodels.ImageCaptureViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {
    private ImageCaptureViewModel viewModel;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private Preview preview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ImageCaptureViewModel.class);
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // Check if we have camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        } else {
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCamera() {
        // To be implemented
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
```

### Next Steps:

1. Implement the layouts for each fragment
2. Complete the ImageCaptureActivity implementation
3. Add the navigation menu for bottom navigation
4. Create utility classes for image processing
5. Implement the full CameraFragment functionality

Once the foundation is in place, continue with implementing the CropFragment and GalleryFragment. 