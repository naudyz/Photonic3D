package org.area515.resinprinter.image;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.SVGConstants;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Created by zyd on 2017/9/29.
 */

public class ConvertSVGToPNG
{
    @Test
    public void testConvert()
    {
        //File srcFile = new File("C:\\Users\\zyd\\uploaddir\\dental16000007.svg");
        //File dstFile = new File("C:\\Users\\zyd\\uploaddir\\dental16000007.png");

        // svg文件路径
        String strSvgURI;
        OutputStream ostream = null;
        try
        {
            // 根据路径获得文件夹
            File fileSvg = new File("C:\\Users\\zyd\\uploaddir\\dental16000007.svg");

            // 构造一个表示此抽象路径名的 file:URI
            URI uri = fileSvg.toURI();

            // 根据此 URI 构造一个 URL
            URL url = uri.toURL();

            // 构造此 URL 的字符串表示形式
            strSvgURI = url.toString();

            // 定义一个通用的转码器的输入
            TranscoderInput input = new TranscoderInput(strSvgURI);

            // 定义图片路径
            String strPngPath = "C:\\Users\\zyd\\uploaddir\\dental16000007.jpg";

            // 输入流
            ostream = new FileOutputStream(strPngPath);

            // 定义单路输出的转码器
            TranscoderOutput output = new TranscoderOutput(ostream);

            // 构造一个新的转码器，产生JPEG图像
            JPEGTranscoder transcoder = new JPEGTranscoder();

            // 设置一个转码过程，JPEGTranscoder.KEY_QUALITY设置输出png的画质精度，0-1之间
            transcoder.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(.8));

            // 转换svg文件为png
            transcoder.transcode(input, output);
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (TranscoderException e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                ostream.flush();
                // 关闭输入流
                ostream.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void transformAndCopy()
    {
        File pngFile = new File("D:\\Users\\zyd\\Desktop\\printFile\\0.png");
        File svgFile = new File("D:\\Users\\zyd\\Desktop\\printFile\\0.svg");
        PNGTranscoder t = new PNGTranscoder();

        try
        {
            TranscodingHints transcoderHints = new TranscodingHints();
            transcoderHints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
            transcoderHints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
            transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
            transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
            transcoderHints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
            transcoderHints.put(ImageTranscoder.KEY_WIDTH, new Float(2560));
            transcoderHints.put(ImageTranscoder.KEY_HEIGHT, new Float(1600));
            transcoderHints.put(ImageTranscoder.KEY_FORCE_TRANSPARENT_WHITE, Boolean.TRUE);
            transcoderHints.put(ImageTranscoder.KEY_BACKGROUND_COLOR, Color.black);

            t.setTranscodingHints(transcoderHints);

            TranscoderInput input = new TranscoderInput(svgFile.toURI().toString());
            pngFile.createNewFile();

            OutputStream ostream = new FileOutputStream(pngFile);
            TranscoderOutput output = new TranscoderOutput(ostream);

            t.transcode(input, output);
            ostream.flush();
            ostream.close();
        }
        catch (IOException e)
        {
            System.out.println(e.toString());
        }
        catch (TranscoderException e)
        {
            System.out.println(e.toString());
        }
    }
}
