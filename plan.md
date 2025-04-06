# Image Capture & Crop Feature - Implementation Plan

## Overview
This plan outlines the implementation of a new standalone activity for image capture, cropping, and management without interfering with the main EventWish app functionality. The feature will:

1. Create a separate app icon for launching the feature directly
2. Allow users to take multiple photos using the camera
3. Provide image cropping functionality for each captured photo
4. Enable reordering of cropped images
5. Export the final result as a single combined image

## 1. Project Structure Additions

### New Activity and Classes
```
app/src/main/java/com/ds/eventwish/imagecapture/
├── ImageCaptureActivity.java          # Main entry point with separate launcher icon
├── fragments/
│   ├── CameraFragment.java            # Camera interface for taking photos
│   ├── CropFragment.java              # Cropping interface for adjusting images
│   └── GalleryFragment.java           # Gallery view to manage all captured images
├── adapters/
│   ├── CapturedImagesAdapter.java     # Adapter for displaying captured images
│   └── GalleryAdapter.java            # Adapter for the gallery with reordering support
├── viewmodels/
│   └── ImageCaptureViewModel.java     # ViewModel to maintain state across fragments
└── utils/
    ├── BitmapUtils.java               # Bitmap manipulation utilities
    ├── CameraUtils.java               # Camera handling utilities
    └── ImageExportUtils.java          # Image export and combination utilities
```

### New Resources
```
app/src/main/res/
├── layout/
│   ├── activity_image_capture.xml     # Main activity layout
│   ├── fragment_camera.xml            # Camera fragment layout
│   ├── fragment_crop.xml              # Crop fragment layout
│   ├── fragment_gallery.xml           # Gallery fragment layout
│   ├── item_captured_image.xml        # Layout for a single captured image in the list
│   └── view_camera_controls.xml       # Camera controls overlay
├── drawable/
│   ├── ic_launcher_camera.xml         # Custom launcher icon for the feature
│   ├── ic_capture_photo.xml           # Camera capture button
│   ├── ic_switch_camera.xml           # Switch camera button
│   ├── ic_crop.xml                    # Crop icon
│   ├── ic_reorder.xml                 # Reorder icon
│   └── ic_export.xml                  # Export icon
└── values/
    ├── strings_camera.xml             # Strings for the camera feature
    └── styles_camera.xml              # Styles for the camera UI
```

## 2. Core Components Implementation

### AndroidManifest.xml Updates
Add a new activity with a separate launcher icon:
```xml
<activity
    android:name=".imagecapture.ImageCaptureActivity"
    android:exported="true"
    android:theme="@style/Theme.EventWish.ImageCapture">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <meta-data
        android:name="android.app.shortcuts"
        android:resource="@xml/shortcuts" />
</activity>
```

Add necessary permissions:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
```

### ImageCaptureActivity.java
Main entry point for the feature:
```java
public class ImageCaptureActivity extends AppCompatActivity {
    private ImageCaptureViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_capture);
        
        viewModel = new ViewModelProvider(this).get(ImageCaptureViewModel.class);
        
        // Check and request permissions
        if (!PermissionUtils.hasCameraPermissions(this)) {
            PermissionUtils.requestCameraPermissions(this);
        } else {
            initializeCamera();
        }
        
        // Set up navigation between fragments
        setupNavigation();
    }
    
    private void initializeCamera() {
        // Navigate to camera fragment
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new CameraFragment())
            .commit();
    }
    
    private void setupNavigation() {
        // Setup bottom navigation or tabs for switching between camera, gallery, etc.
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Handle permission results
    }
}
```

### ImageCaptureViewModel.java
Maintain state across different fragments:
```java
public class ImageCaptureViewModel extends ViewModel {
    // LiveData to store captured images
    private final MutableLiveData<List<CapturedImage>> capturedImages = new MutableLiveData<>(new ArrayList<>());
    
    // Store current image being edited/cropped
    private final MutableLiveData<Integer> currentImageIndex = new MutableLiveData<>(-1);
    
    // Class to represent a captured image
    public static class CapturedImage {
        private Uri originalUri;
        private Uri croppedUri;
        private boolean isCropped = false;
        
        // Getters and setters
    }
    
    // Methods to add, remove, reorder images
    public void addCapturedImage(Uri imageUri) {
        List<CapturedImage> currentList = capturedImages.getValue();
        CapturedImage newImage = new CapturedImage();
        newImage.setOriginalUri(imageUri);
        currentList.add(newImage);
        capturedImages.setValue(currentList);
    }
    
    public void reorderImages(int fromPosition, int toPosition) {
        List<CapturedImage> currentList = capturedImages.getValue();
        CapturedImage movedItem = currentList.remove(fromPosition);
        currentList.add(toPosition, movedItem);
        capturedImages.setValue(currentList);
    }
    
    // Methods to update cropped image
    public void setCurrentImageCropped(Uri croppedUri) {
        // Update the current image with its cropped version
    }
    
    // Methods to export the final combined image
    public void exportCombinedImage(Context context, ExportCallback callback) {
        // Logic to combine images and export
    }
    
    public interface ExportCallback {
        void onExportSuccess(Uri exportedImageUri);
        void onExportError(Exception e);
    }
}
```

## 3. Camera Implementation

### CameraFragment.java
Handle camera preview and photo capture:
```java
public class CameraFragment extends Fragment {
    private ImageCaptureViewModel viewModel;
    private CameraX cameraProvider;
    private Preview preview;
    private ImageCapture imageCapture;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(ImageCaptureViewModel.class);
        
        // Set up CameraX
        setupCamera();
        
        // Set up capture button
        view.findViewById(R.id.capture_button).setOnClickListener(v -> {
            captureImage();
        });
        
        // Set up switch to gallery button
        view.findViewById(R.id.gallery_button).setOnClickListener(v -> {
            navigateToGallery();
        });
    }
    
    private void setupCamera() {
        // Initialize CameraX components
    }
    
    private void captureImage() {
        // Take photo and save to file
        File photoFile = createImageFile();
        
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(
            outputOptions,
            getExecutor(),
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                    Uri savedUri = Uri.fromFile(photoFile);
                    // Add image to ViewModel
                    viewModel.addCapturedImage(savedUri);
                    // Navigate to crop fragment
                    navigateToCrop(savedUri);
                }
                
                @Override
                public void onError(ImageCaptureException exception) {
                    // Handle error
                }
            }
        );
    }
    
    private void navigateToCrop(Uri imageUri) {
        // Navigate to crop fragment
        CropFragment cropFragment = CropFragment.newInstance(imageUri);
        requireActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, cropFragment)
            .addToBackStack(null)
            .commit();
    }
    
    private void navigateToGallery() {
        // Navigate to gallery fragment
        requireActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new GalleryFragment())
            .addToBackStack(null)
            .commit();
    }
}
```

## 4. Image Cropping

### CropFragment.java
Provide image cropping functionality:
```java
public class CropFragment extends Fragment {
    private ImageCaptureViewModel viewModel;
    private Uri imageUri;
    private CropImageView cropImageView;
    
    public static CropFragment newInstance(Uri imageUri) {
        CropFragment fragment = new CropFragment();
        Bundle args = new Bundle();
        args.putParcelable("imageUri", imageUri);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUri = getArguments().getParcelable("imageUri");
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crop, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(ImageCaptureViewModel.class);
        
        // Set up crop view
        cropImageView = view.findViewById(R.id.cropImageView);
        cropImageView.setImageUriAsync(imageUri);
        
        // Set up crop button
        view.findViewById(R.id.crop_button).setOnClickListener(v -> {
            cropImage();
        });
        
        // Set up back button
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });
        
        // Set up done button (to return to camera or gallery)
        view.findViewById(R.id.done_button).setOnClickListener(v -> {
            navigateBack();
        });
    }
    
    private void cropImage() {
        Uri croppedImageUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg"));
        
        cropImageView.saveCroppedImageAsync(croppedImageUri, Bitmap.CompressFormat.JPEG, 90);
        cropImageView.setOnCropImageCompleteListener((view, result) -> {
            if (result.isSuccessful()) {
                // Update ViewModel with cropped image
                viewModel.setCurrentImageCropped(result.getUriContent());
                // Show success message
                Toast.makeText(requireContext(), "Image cropped successfully", Toast.LENGTH_SHORT).show();
            } else {
                // Show error
                Toast.makeText(requireContext(), "Failed to crop image", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void navigateBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
```

## 5. Gallery & Image Management

### GalleryFragment.java
Manage captured images and their order:
```java
public class GalleryFragment extends Fragment implements GalleryAdapter.OnItemActionListener {
    private ImageCaptureViewModel viewModel;
    private GalleryAdapter adapter;
    private RecyclerView recyclerView;
    private ItemTouchHelper itemTouchHelper;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(ImageCaptureViewModel.class);
        
        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        adapter = new GalleryAdapter(this);
        recyclerView.setAdapter(adapter);
        
        // Set up ItemTouchHelper for drag & drop reordering
        ItemTouchHelper.Callback callback = new GalleryItemTouchHelperCallback(adapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        
        // Observe captured images
        viewModel.getCapturedImages().observe(getViewLifecycleOwner(), capturedImages -> {
            adapter.submitList(new ArrayList<>(capturedImages));
        });
        
        // Set up export button
        view.findViewById(R.id.export_button).setOnClickListener(v -> {
            exportImages();
        });
        
        // Set up back to camera button
        view.findViewById(R.id.camera_button).setOnClickListener(v -> {
            navigateToCamera();
        });
    }
    
    @Override
    public void onItemClick(int position) {
        // Navigate to crop fragment for this image
        Uri imageUri = viewModel.getCapturedImages().getValue().get(position).getDisplayUri();
        navigateToCrop(imageUri, position);
    }
    
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }
    
    @Override
    public void onItemDelete(int position) {
        viewModel.removeImage(position);
    }
    
    private void navigateToCrop(Uri imageUri, int position) {
        viewModel.setCurrentImageIndex(position);
        CropFragment cropFragment = CropFragment.newInstance(imageUri);
        requireActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, cropFragment)
            .addToBackStack(null)
            .commit();
    }
    
    private void navigateToCamera() {
        requireActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new CameraFragment())
            .commit();
    }
    
    private void exportImages() {
        if (viewModel.getCapturedImages().getValue().isEmpty()) {
            Toast.makeText(requireContext(), "No images to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress indicator
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Exporting images...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        viewModel.exportCombinedImage(requireContext(), new ImageCaptureViewModel.ExportCallback() {
            @Override
            public void onExportSuccess(Uri exportedImageUri) {
                progressDialog.dismiss();
                // Show success message and share options
                showExportSuccessDialog(exportedImageUri);
            }
            
            @Override
            public void onExportError(Exception e) {
                progressDialog.dismiss();
                // Show error message
                Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void showExportSuccessDialog(Uri exportedImageUri) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Export Successful")
            .setMessage("Your images have been combined and saved. What would you like to do?")
            .setPositiveButton("Share", (dialog, which) -> {
                shareImage(exportedImageUri);
            })
            .setNegativeButton("View", (dialog, which) -> {
                viewImage(exportedImageUri);
            })
            .setNeutralButton("Done", (dialog, which) -> {
                dialog.dismiss();
            })
            .show();
    }
    
    private void shareImage(Uri imageUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        startActivity(Intent.createChooser(shareIntent, "Share image using"));
    }
    
    private void viewImage(Uri imageUri) {
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setDataAndType(imageUri, "image/*");
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(viewIntent);
    }
}
```

## 6. Adapters Implementation

### GalleryAdapter.java
```java
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ImageViewHolder> implements ItemTouchHelperAdapter {
    private List<ImageCaptureViewModel.CapturedImage> images = new ArrayList<>();
    private final OnItemActionListener listener;
    
    public interface OnItemActionListener {
        void onItemClick(int position);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
        void onItemDelete(int position);
    }
    
    public GalleryAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_captured_image, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageCaptureViewModel.CapturedImage image = images.get(position);
        
        // Load image using Glide
        Glide.with(holder.itemView.getContext())
            .load(image.getDisplayUri())
            .centerCrop()
            .into(holder.imageView);
        
        // Set up click listeners
        holder.itemView.setOnClickListener(v -> listener.onItemClick(holder.getBindingAdapterPosition()));
        
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                listener.onStartDrag(holder);
            }
            return false;
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            listener.onItemDelete(holder.getBindingAdapterPosition());
        });
    }
    
    @Override
    public int getItemCount() {
        return images.size();
    }
    
    public void submitList(List<ImageCaptureViewModel.CapturedImage> newList) {
        this.images = newList;
        notifyDataSetChanged();
    }
    
    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        // Use ViewModel to update the order
        // Notify adapter of change
    }
    
    @Override
    public void onItemDismiss(int position) {
        // Not used for this implementation
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView dragHandle;
        ImageView deleteButton;
        
        ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            dragHandle = itemView.findViewById(R.id.drag_handle);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
    
    public interface ItemTouchHelperAdapter {
        void onItemMove(int fromPosition, int toPosition);
        void onItemDismiss(int position);
    }
}
```

## 7. Utility Classes

### BitmapUtils.java
```java
public class BitmapUtils {
    /**
     * Combine multiple bitmaps vertically into a single bitmap
     */
    public static Bitmap combineImagesVertically(List<Bitmap> bitmaps) {
        if (bitmaps == null || bitmaps.isEmpty()) return null;
        
        // Calculate dimensions
        int width = 0;
        int height = 0;
        
        for (Bitmap bitmap : bitmaps) {
            width = Math.max(width, bitmap.getWidth());
            height += bitmap.getHeight();
        }
        
        // Create a new bitmap
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        // Draw each bitmap
        int currentHeight = 0;
        for (Bitmap bitmap : bitmaps) {
            canvas.drawBitmap(bitmap, 0, currentHeight, null);
            currentHeight += bitmap.getHeight();
        }
        
        return result;
    }
    
    /**
     * Save bitmap to file
     */
    public static Uri saveBitmapToFile(Context context, Bitmap bitmap, String fileName) {
        File file = new File(context.getCacheDir(), fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            return FileProvider.getUriForFile(
                context, 
                context.getPackageName() + ".fileprovider", 
                file
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Load bitmap from Uri
     */
    public static Bitmap loadBitmapFromUri(Context context, Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }
}
```

## 8. Implementation Timeline

1. **Week 1: Setup and Basic Structure**
   - Create project structure and resource files
   - Implement ActivityImageCapture and ViewModel
   - Configure AndroidManifest.xml with necessary permissions
   - Create separate launcher icon for the feature

2. **Week 2: Camera Implementation**
   - Implement CameraFragment for image capture
   - Set up camera permissions handling
   - Implement basic image storage

3. **Week 3: Cropping Implementation**
   - Implement CropFragment with cropping interface
   - Create image manipulation utilities
   - Set up image storage between fragments

4. **Week 4: Gallery and Reordering**
   - Implement GalleryFragment for image management
   - Create drag-and-drop reordering functionality
   - Implement deletion of images

5. **Week 5: Export and Sharing**
   - Implement image combining functionality
   - Create export and sharing options
   - Add image saving to device storage

6. **Week 6: Polish and Testing**
   - UI/UX improvements
   - Bug fixes and performance optimizations
   - Testing across different devices and Android versions
   - Documentation

## 9. Additional Feature Ideas

1. **Filters and Effects**
   - Add photo filters (grayscale, sepia, etc.)
   - Implement basic image adjustments (brightness, contrast)

2. **Text Annotations**
   - Allow adding text to images
   - Provide font selection and styling options

3. **Drawing and Markup**
   - Add drawing capabilities on images
   - Implement shapes and arrows for marking up images

4. **Templates and Layouts**
   - Provide templates for image arrangements
   - Allow custom grid layouts for multiple images

5. **Integration with Main App**
   - Option to use captured images in EventWish templates
   - Share images to EventWish functionality

## 10. Testing Strategy

1. **Unit Tests**
   - Test image manipulation utilities
   - Test ViewModel logic for image management

2. **Integration Tests**
   - Test camera capture to crop workflow
   - Test gallery management and reordering

3. **UI Tests**
   - Test user interactions with camera controls
   - Test cropping gestures and controls
   - Test drag-and-drop reordering

4. **Device Compatibility**
   - Test on various Android versions
   - Test on different screen sizes and resolutions
   - Test with different camera hardware

## 11. Security Considerations

1. **Permission Handling**
   - Implement proper permission requests
   - Handle permission denial gracefully

2. **Storage Security**
   - Use scoped storage for Android 10+
   - Ensure proper file cleanup after use

3. **Privacy**
   - Clear temporary files when app is closed
   - Don't store metadata that could compromise privacy

## 12. Performance Optimization

1. **Memory Management**
   - Recycle bitmaps when no longer needed
   - Use subsampling for large images

2. **Processing Optimization**
   - Run heavy image processing in background threads
   - Use efficient image processing algorithms

3. **Storage Efficiency**
   - Use appropriate compression for saved images
   - Clean up temporary files regularly 