package module.opencv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import module.bot.basename
import module.thisLogger
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.opencv.opencv_java
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 *
 * @author caseycheng
 * @date 2023/10/16-17:57
 * @doc 不知道，不认识，第一次用，函数看上去有点见鬼是正常的。
 **/
object OpenCVService {
    fun init() {
        Loader.load(opencv_java::class.java)
        logger.info("opencv is ready")
    }

    private val logger = thisLogger<OpenCVService>()
    suspend fun bgr2rgb(path: String): Mat {
        return conversion(path, Imgproc.COLOR_BGR2RGB)
    }

    suspend fun bgr2gray(path: String): Mat {
        return conversion(path, Imgproc.COLOR_BGR2GRAY)
    }

    /**
     * 将webm转换为gif
     * @param fileName 文件路径
     * @param to 保存到的位置
     */
    suspend fun webmToGifFile(fileName: String, to: String) {
        val (file,suffix) = Path(fileName).toAbsolutePath()
            .let { it.pathString to it.basename().drop(it.basename().lastIndexOf('.') + 1) }
        val grabber = FFmpegFrameGrabber(file)
        grabber.start()
        val frame:Frame

    }

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
//            val format = to.drop(to.lastIndexOf('.') + 1)

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