package github.cheng.module.opencv

import github.cheng.module.ShutDown
import github.cheng.module.bot.basename
import github.cheng.module.bot.suffix
import github.cheng.module.thisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.opencv_java
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 *
 * @author caseycheng
 * @date 2023/10/16-17:57
 * @doc 不知道，不认识，第一次用，函数看上去有点见鬼是正常的。
 **/
object OpenCVService {
    private val frameConverter = OpenCVFrameConverter.ToMat()

    fun init() {
        Loader.load(opencv_java::class.java)
        logger.info("opencv is ready")
        ShutDown.addShutDownHook("opencv") {
            frameConverter.close()
        }
    }

    private val logger = thisLogger<OpenCVService>()
    suspend fun bgr2rgb(path: String): Mat {
        return conversion(path, Imgproc.COLOR_BGR2RGB)
    }

    suspend fun bgr2gray(path: String): Mat {
        return conversion(path, Imgproc.COLOR_BGR2GRAY)
    }

    /**
     * @param frameSpeed 数字越小 速度越快
     */
    suspend fun videoToGif(
        src: Path,
        savePath: Path,
        frameSpeed: Int = 3
    ) {
        withContext(Dispatchers.Default) {
            try {
                if (savePath.basename().suffix().uppercase() == "GIF") {
                    FFmpegFrameGrabber(src.toFile())
                        .use { grabber ->
                            grabber.start()
                            val saveFile = savePath.toFile()
                            if (!saveFile.exists()) {
                                saveFile.createNewFile()
                            }
                            withContext(Dispatchers.IO) {
                                saveFile.createNewFile()
                            }
                            FFmpegFrameRecorder(saveFile, grabber.imageWidth, grabber.imageHeight)
                                .use { recorder ->
                                    recorder.videoCodec = avcodec.AV_CODEC_ID_GIF
                                    recorder.pixelFormat = avutil.AV_PIX_FMT_RGB8
                                    recorder.start()

                                    var frame: Frame?
                                    do {
                                        frame = grabber.grabImage()
                                        frame?.let {
                                            try {
                                                (0..<frameSpeed).forEach { _ -> recorder.record(frame) }
                                            } catch (_: Exception) {

                                            }

                                        }
                                    } while (frame != null)
                                    grabber.stop()
                                }
                        }

                } else {
                    throw FileNotSupport("$src not is gif file")
                }
            } catch (e: Throwable) {
                throw e
            }
        }

    }


    @Deprecated("这是一个废弃的方法，它没有将image和video分开，并且webp也不用转换")
    suspend fun conversionImageFile(
        path: String,
        to: String,
        width: Double,
        height: Double,
        scale: Double
    ) {
        withContext(Dispatchers.IO) {
            val path = Path(path).toAbsolutePath().pathString
            val to = Path(to).toAbsolutePath().pathString

            logger.info("[openCV] conversionImageFile $path $to")
            val src = Imgcodecs.imread(path)
            if (src.empty()) throw FileNotSupport("Not support file $path")
            val dst = Mat()
            val size = Size(width * scale, height * scale)
            withContext(Dispatchers.Default) {
                if (src.empty()) {
                    logger.info("[openCV] conversionImageFile $path is empty")
                    throw RuntimeException()
                } else {
                    logger.info("[openCV] conversionImageFile $path is not empty")
                }
                Imgproc.resize(src, dst, size)
            }
            withContext(Dispatchers.IO) {
                Imgcodecs.imwrite(to, dst)
            }
        }
    }

    suspend fun conversion(path: String, code: Int): Mat {
        val image: Mat
        val result = Mat()
        withContext(Dispatchers.IO) {
            image = Imgcodecs.imread(path)
        }
        withContext(Dispatchers.Default) {
            Imgproc.cvtColor(image, result, code)
        }
        return result
    }


}

class FileNotSupport(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)
