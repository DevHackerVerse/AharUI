package com.example.aharui.ui.diet

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.aharui.R
import com.example.aharui.data.api.GeminiService
import com.example.aharui.data.model.Meal
import com.example.aharui.data.model.MealSource
import com.example.aharui.data.model.MealType
import com.example.aharui.util.OCRHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class ScanFoodFragment : Fragment() {

    private val dietViewModel: DietViewModel by viewModels()

    @Inject
    lateinit var geminiService: GeminiService

    private lateinit var ocrHelper: OCRHelper
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraPreview: PreviewView
    private lateinit var captureButton: Button
    private lateinit var progressBar: ProgressBar

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera permission is required to scan food.",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigateUp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ocrHelper = OCRHelper(geminiService)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan_food, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraPreview = view.findViewById(R.id.camera_preview)
        captureButton = view.findViewById(R.id.capture_button)
        progressBar = view.findViewById(R.id.progress_bar)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Failed to start camera.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            requireContext().externalCacheDir,
            "${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        captureButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    processImage(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    captureButton.isEnabled = true
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Photo capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun processImage(imageFile: File) {
        lifecycleScope.launch {
            try {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

                // Step 1: Extract text using ML Kit OCR
                val ocrResult = ocrHelper.extractTextFromImage(bitmap)

                if (ocrResult is com.example.aharui.util.Result.Success) {
                    val extractedText = ocrResult.data

                    // Step 2: Enhanced parsing using Gemini AI
                    Toast.makeText(
                        requireContext(),
                        "Analyzing nutrition info with AI...",
                        Toast.LENGTH_SHORT
                    ).show()

                    val aiResult = ocrHelper.parseNutritionInfoWithAI(extractedText)

                    progressBar.visibility = View.GONE
                    captureButton.isEnabled = true

                    when (aiResult) {
                        is com.example.aharui.util.Result.Success -> {
                            val nutritionInfo = aiResult.data
                            showEnhancedNutritionDialog(nutritionInfo, extractedText)
                        }
                        is com.example.aharui.util.Result.Error -> {
                            // Fallback to regex parsing if AI fails
                            val basicInfo = ocrHelper.parseNutritionInfo(extractedText)
                            showBasicNutritionDialog(basicInfo, extractedText)
                        }
                        else -> {
                            Toast.makeText(
                                requireContext(),
                                "Failed to extract nutrition info",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    progressBar.visibility = View.GONE
                    captureButton.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Failed to extract text from image",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Clean up
                imageFile.delete()

            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                captureButton.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Error processing image: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showEnhancedNutritionDialog(
        nutritionInfo: com.example.aharui.data.api.EnhancedNutritionInfo,
        extractedText: String
    ) {
        val mealTypes = MealType.values().map { it.name.lowercase().capitalize() }
        var selectedMealType = MealType.BREAKFAST

        val confidenceEmoji = when (nutritionInfo.confidence) {
            "high" -> "✅"
            "medium" -> "⚠️"
            else -> "❓"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("AI Detected Nutrition Info $confidenceEmoji")
            .setMessage("""
                Food: ${nutritionInfo.foodName}
                Serving: ${nutritionInfo.servingSize}
                
                Calories: ${nutritionInfo.calories} kcal
                Protein: ${nutritionInfo.proteinG}g
                Carbs: ${nutritionInfo.carbsG}g
                Fat: ${nutritionInfo.fatG}g
                
                Confidence: ${nutritionInfo.confidence.uppercase()}
                
                Raw Text (first 150 chars):
                ${extractedText.take(150)}${if (extractedText.length > 150) "..." else ""}
            """.trimIndent())
            .setSingleChoiceItems(mealTypes.toTypedArray(), 0) { _, which ->
                selectedMealType = MealType.values()[which]
            }
            .setPositiveButton("Add to Log") { dialog, _ ->
                val meal = Meal(
                    name = nutritionInfo.foodName,
                    calories = nutritionInfo.calories,
                    mealType = selectedMealType,
                    source = MealSource.OCR,
                    proteinG = nutritionInfo.proteinG,
                    carbsG = nutritionInfo.carbsG,
                    fatG = nutritionInfo.fatG,
                    quantity = nutritionInfo.servingSize
                )
                dietViewModel.addMeal(meal)
                dialog.dismiss()
                findNavController().navigateUp()
            }
            .setNeutralButton("Edit") { dialog, _ ->

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showBasicNutritionDialog(
        nutritionInfo: OCRHelper.NutritionInfo,
        extractedText: String
    ) {
        val mealTypes = MealType.values().map { it.name.lowercase().capitalize() }
        var selectedMealType = MealType.BREAKFAST

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Detected Nutrition Info (Basic)")
            .setMessage("""
                Calories: ${nutritionInfo.calories} kcal
                Protein: ${nutritionInfo.proteinG}g
                Carbs: ${nutritionInfo.carbsG}g
                Fat: ${nutritionInfo.fatG}g
                
                Note: Using basic pattern matching.
                Consider manually verifying the values.
                
                Extracted Text (first 150 chars):
                ${extractedText.take(150)}${if (extractedText.length > 150) "..." else ""}
            """.trimIndent())
            .setSingleChoiceItems(mealTypes.toTypedArray(), 0) { _, which ->
                selectedMealType = MealType.values()[which]
            }
            .setPositiveButton("Add to Log") { dialog, _ ->
                val meal = Meal(
                    name = "Scanned Food",
                    calories = nutritionInfo.calories,
                    mealType = selectedMealType,
                    source = MealSource.OCR,
                    proteinG = nutritionInfo.proteinG,
                    carbsG = nutritionInfo.carbsG,
                    fatG = nutritionInfo.fatG
                )
                dietViewModel.addMeal(meal)
                dialog.dismiss()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}