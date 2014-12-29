package jp.classmethod.sample.jpgtomp4;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.RgbToYuv420;

import android.graphics.Bitmap;

public class JPGToMP4 {

	SeekableByteChannel sink;
	FramesMP4MuxerTrack outTrack;
	
	public void imageToMP4Quick(File src, File out) throws IOException {
		sink = NIOUtils.writableFileChannel(out);

		MP4Muxer muxer = new MP4Muxer(sink, Brand.MP4);

		FramesMP4MuxerTrack outTrack = muxer.addTrackForCompressed(TrackType.VIDEO, 1);

		outTrack.addFrame(new MP4Packet(NIOUtils.fetchFrom(src), 0, 1, 3, 0, true, null, 0, 0));
		//outTrack.addFrame(new MP4Packet(NIOUtils.fetchFrom(src), 3, 1, 3, 1, true, null, 3, 0));

		outTrack.addSampleEntry(MP4Muxer.videoSampleEntry("png ", new Size(400, 300), "JCodec"));

		muxer.writeHeader();

		NIOUtils.closeQuietly(sink);
		
	}
	
	public void imageToMP4(Picture src1, Picture src2, File out) throws IOException {
//	    // A transform to convert RGB to YUV colorspace
	    RgbToYuv420 transform = new RgbToYuv420(0, 0);
	    Picture toEncode1 = Picture.create(src1.getWidth(), src1.getHeight(), ColorSpace.YUV420);
	    transform.transform(src1, toEncode1);

	    Picture toEncode2 = Picture.create(src2.getWidth(), src2.getHeight(), ColorSpace.YUV420);
	    transform.transform(src2, toEncode2);

//
//	    // A JCodec native picture that would hold source image in YUV colorspace
//
//	    // Perform conversion
//	    transform.transform(AWTUtil.fromBufferedImage(bi), yuv);

	    // Create MP4 muxer
		sink = NIOUtils.writableFileChannel(out);
		
	    MP4Muxer muxer = new MP4Muxer(sink, Brand.MP4);

	    // Create H.264 encoder
	    H264Encoder encoder = new H264Encoder();

	    // Allocate a buffer that would hold an encoded frame
	    ByteBuffer _out1 = ByteBuffer.allocate(toEncode1.getWidth() * toEncode1.getHeight() * 6);//ine.getWidth() * ine.getHeight() * 6);
	    ByteBuffer _out2 = ByteBuffer.allocate(toEncode2.getWidth() * toEncode2.getHeight() * 6);//ine.getWidth() * ine.getHeight() * 6);

	    // Allocate storage for SPS/PPS, they need to be stored separately in a special place of MP4 file
	    List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
	    List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();

	    // Encode image into H.264 frame, the result is stored in '_out' buffer
	    ByteBuffer result1 = encoder.encodeFrame(_out1, toEncode1);
	    ByteBuffer result2 = encoder.encodeFrame(_out2, toEncode2);

	    // Based on the frame above form correct MP4 packet
	    H264Utils.encodeMOVPacket(result1, spsList, ppsList);
	    H264Utils.encodeMOVPacket(result2, spsList, ppsList);

	    // Add a video track
	    outTrack = muxer.addTrackForCompressed(TrackType.VIDEO, 1);

	    // Add packet to video track
	    outTrack.addFrame(new MP4Packet(result1, 0, 1, 2, 0, true, null, 0, 0));
	    outTrack.addFrame(new MP4Packet(result2, 2, 1, 2, 1, true, null, 2, 0));

	    // Push saved SPS/PPS to a special storage in MP4
	    outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList));

	    // Write MP4 header and finalize recording
	    muxer.writeHeader();
	    NIOUtils.closeQuietly(sink);
	}
	
	
	public static Picture fromBitmap(Bitmap src) {
	    Picture dst = Picture.create((int)src.getWidth(), (int)src.getHeight(), ColorSpace.RGB);
	    fromBitmap(src, dst);
	    return dst;
	}

	public static void fromBitmap(Bitmap src, Picture dst) {
	    int[] dstData = dst.getPlaneData(0);
	    int[] packed = new int[src.getWidth() * src.getHeight()];

	    src.getPixels(packed, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());

	    for (int i = 0, srcOff = 0, dstOff = 0; i < src.getHeight(); i++) {
	        for (int j = 0; j < src.getWidth(); j++, srcOff++, dstOff += 3) {
	            int rgb = packed[srcOff];
	            dstData[dstOff]     = (rgb >> 16) & 0xff;
	            dstData[dstOff + 1] = (rgb >> 8) & 0xff;
	            dstData[dstOff + 2] = rgb & 0xff;
	        }
	    }
	}
}
