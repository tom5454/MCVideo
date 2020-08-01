package com.tom.mcvideo.transcoder;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

public class FFMpegTranscoder {
	private static String marker1 = "Output #0";
	private static String marker2 = "Stream #0";
	private static byte[] data = DatatypeConverter.parseHexBinary("77673331746730311FDF65321DEBDE811EE8DE828585EA828686EA7E8686EA7ECC673331C36730326F5F66324FE080821EE87C8285E9EA8286867D7E86867E7E8C633031CB6330326F5666324FB644824CE17C824D057D464E867D7E4E867E7E8ED46132D3D46132A3BA62324B9314824C0B45824D067D164E067D7E4E4E7E7E13D04032D0D5404295D64142A02A1415480B25164E061A164E06207E4E4E7E7E10D14042D1D2414296AA4142A12A5142A2081A164878381649790E214A4A0A2111D2414211D24142D2D251423D3E5142A23E521649799216497909214A7A0A221212414212D24242D2D251423E3E52423E3E5216497992214A7A0A3A4A7A0A22");

	public static void main(String[] args) throws IOException {
		System.out.println("FFMpeg Transcoder starting...");
		File f = new File("transcoder.properties");
		boolean runInit = !f.exists() || (args.length > 0 && "--config".equals(args[0]));
		PropertyManager m = new PropertyManager(f);
		{
			BufferedReader rd = new BufferedReader(new InputStreamReader(System.in));
			if(runInit) {
				System.out.println("=== FFMpeg Transcoder Configuration Wizard ===");
				readProperty(m, rd, "Output Server Address:", "output", "localhost:9050");
				readProperty(m, rd, "Screen Name", "name", "");
				readProperty(m, rd, "Screen Resolution '<width>x<height>'", "resolution", "");
				//readProperty(m, rd, "Palette path", "palette", "default_map.properties");
				readProperty(m, rd, "FFMpeg path", "path", "ffmpeg.exe");
				System.out.println("FFMpeg input settings");
				System.out.println("  File input: '-i <file name>'");
				System.out.println("Compatible with any ffmpeg input settings");
				readProperty(m, rd, "FFMpeg arguments", "args", "");
			}
		}
		System.out.println("Loading configuration...");
		String res = m.getStringProperty("resolution", "");
		String[] sp = res.split("x");
		if(sp.length != 2) {
			System.err.println("Malformed resolution: '" + res + "' expected '<width>x<height>'");
			return;
		}
		int w = Integer.parseInt(sp[0]);
		int h = Integer.parseInt(sp[1]);

		List<String> pl = new ArrayList<>();
		pl.add(m.getStringProperty("path", "ffmpeg.exe"));
		//pl.add("-re");
		StringBuilder bb = new StringBuilder();
		boolean esc1 = false, esc2 = false;
		for (char c : m.getStringProperty("args", "").toCharArray()) {
			if(esc2) {
				esc2 = false;
				bb.append(c);
				continue;
			}
			if(c == '"') {
				if(esc1) {
					esc1 = false;
				} else {
					if(bb.length() != 0)throw new RuntimeException("failed to parse args, invalid '\"' character");
					esc1 = true;
				}
			} else if(c == '\\') {
				esc2 = true;
			} else if(c == ' ' && !esc1) {
				pl.add(bb.toString());
				bb = new StringBuilder();
			} else {
				bb.append(c);
			}
		}
		if(bb.length() > 0)pl.add(bb.toString());
		String name = m.getStringProperty("name", "");
		String ip = m.getStringProperty("output", "localhost:9050");
		int fps = m.getIntProperty("fps", 20);
		if(fps < 1 || fps > 20)throw new RuntimeException("FPS value out of range (1-20): " + fps);
		m.saveProperties();
		pl.add("-filter:v");
		pl.add("scale=" + w + ":-1");
		pl.add("-c:v");
		pl.add("rawvideo");
		pl.add("-pix_fmt");
		pl.add("rgb8");
		pl.add("-framerate");
		pl.add(Integer.toString(fps));
		pl.add("-an");
		pl.add("-f");
		pl.add("rawvideo");
		pl.add("pipe:1");
		String[] ips = ip.split(":");
		ip = ips[0];
		int port = 9050;
		if(ips.length > 1) {
			port = Integer.parseInt(ips[1]);
		}
		System.out.println("Loaded configuration, connecting to server...");
		try (Socket s = new Socket(ip, port)){
			DataInputStream is = new DataInputStream(s.getInputStream());
			DataOutputStream dout = new DataOutputStream(s.getOutputStream());
			byte[] dt = name.getBytes();
			dout.writeShort(dt.length);
			dout.write(dt);
			if(is.readByte() != 1)throw new IOException();
			System.out.println("Connected to server, starting FFMPEG...");
			System.out.println(pl.stream().collect(Collectors.joining(" ", "> ", "")));
			//int w = 640;//1024
			ProcessBuilder pb = new ProcessBuilder(pl);
			new Thread("System Input Reader Thread") {
				@Override
				public void run() {
					try {
						System.in.read();
					} catch (IOException e) {
					}
					try {
						s.close();
					} catch (IOException e) {
					}
					System.out.println("FFMpeg Transcoder exit");
					System.exit(0);
				}
			}.start();
			Process p = pb.start();
			BufferedReader perr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String ln;
			while((ln = perr.readLine()) != null) {
				System.out.println("[FFMPEG] " + ln);
				if(ln.startsWith(marker1))break;
			}
			while((ln = perr.readLine()) != null) {
				System.out.println("[FFMPEG] " + ln);
				if(ln.trim().startsWith(marker2))break;
			}
			System.out.println("Output settings: " + ln.trim());
			sp = ln.trim().split(",");
			sp = sp[2].trim().split(" ");
			res = sp[0].trim();
			sp = res.split("x");
			w = Integer.parseInt(sp[0]);
			h = Integer.parseInt(sp[1]);

			new Thread("FFMpeg Logger Thread") {
				@Override
				public void run() {
					try {
						String ln;
						while((ln = perr.readLine()) != null) {
							System.out.println("[FFMPEG] " + ln);
						}
					} catch (IOException e) {
					}
				}
			}.start();
			System.out.println("FFMpeg transcoder running");
			try (DataInputStream pin = new DataInputStream(p.getInputStream())) {
				byte[] frame = new byte[w*h];//360 640*400 w*((w * 10)/16)
				long time = System.currentTimeMillis();
				//pin.readFully(new byte[200]);
				while(true) {
					pin.readFully(frame);
					dout.writeInt(w);
					dout.writeInt(h);//(w * 10) / 16
					for (int i = 0; i < frame.length; i++) {
						frame[i] = data[Byte.toUnsignedInt(frame[i])];
					}
					dout.write(frame);
					long sleep = (1000 / fps) - (System.currentTimeMillis() - time) - 2;
					time = System.currentTimeMillis();
					if(sleep > 1)Thread.sleep(sleep);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("FFMpeg Transcoder exit");
		System.exit(0);
	}

	private static void readProperty(PropertyManager m, BufferedReader rd, String name, String key, String def) throws IOException {
		System.out.println(name);
		System.out.println("Default value: '" + def + "'");
		if(m.hasProperty(key))System.out.println("Current value: '" + m.getStringProperty(key, def) + "'");
		String ln = rd.readLine();
		if(ln.isEmpty())m.getStringProperty(key, def);
		else m.setProperty(key, ln);
	}
}
