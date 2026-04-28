# ModelNexus

> **Credits**: Inspired by the original [ModelHub](https://github.com/conscious-engines/modelhub) project. Special thanks to the **Conscious Engines** team for the design inspiration and core architectural concepts.

ModelNexus is a premium Kotlin Multiplatform (KMP) application designed to unify your AI model library across Windows and Linux.

## Features
- **Centralized Library**: Scans multiple sources to give you a single view of all your models.
    - **LM Studio**: Automatically detects models in `~/.lmstudio/models`.
    - **Hugging Face Hub**: Scans the HF cache at `~/.cache/huggingface/hub`.
    - **Ollama**: Supports Ollama model manifests in `~/.ollama/models`.
    - **Universal PC Scan**: Scans `Downloads`, `Documents`, and `Desktop` for `.gguf`, `.safetensors`, and `.task` files.
    - **Custom Folders**: Add your own model directories in the Settings panel.
- **Remote Explore**: Search the entire Hugging Face Hub directly from the app.
- **Intelligent Metadata**:
    - Automatic name cleaning (e.g., `Llama-3-8B-it` -> `Llama 3 8B IT`).
    - Format detection (GGUF, MLX, Safetensors, Task).
    - Quantization extraction.
- **Real-time Status**:
    - **Liveness Indicator**: Pulsing green dot for models currently loaded in LM Studio.
    - **Source Badges**: Color-coded badges for LM, HF, OL, and PC sources.
- **Modern UI**:
    - Built with **Compose Multiplatform**.
    - Glassmorphic dark theme.
    - Smooth animations and hover effects.
    - Click-to-copy model identifiers.

## Getting Started

### Prerequisites
- **Java JDK 17** or higher.
- (Optional) **LM Studio** running on `localhost:1234` for liveness indicators.

### Running the App
Navigate to the `app` directory and run:
```powershell
./gradlew :composeApp:run
```

### Building for Distribution
To create an executable for your OS:
```powershell
# For Windows (MSI)
./gradlew :composeApp:packageMsi

# For Linux (DEB)
./gradlew :composeApp:packageDeb
```

## Technology Stack
- **Language**: Kotlin 2.3.20
- **UI Framework**: Compose Multiplatform 1.10.3
- **Serialization**: kotlinx-serialization
- **Networking**: Java HTTP Client (Standard)

## License
MIT
