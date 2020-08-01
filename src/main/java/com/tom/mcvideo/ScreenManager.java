package com.tom.mcvideo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

public class ScreenManager extends Thread {
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	public static final String ID = "mcvideo_screenmanager";
	private State state;

	public class State extends PersistentState {
		private World world;
		public State(World world) {
			super(ID);
			this.world = world;
			state = this;
		}

		@Override
		public void fromTag(CompoundTag tag) {
			ListTag l = tag.getList("screens", 10);
			for (int i = 0; i < l.size(); i++) {
				CompoundTag t = l.getCompound(i);
				addScreen(t.getString("name"), new MapScreen(world, t));
			}
		}

		@Override
		public CompoundTag toTag(CompoundTag tag) {
			ListTag l = new ListTag();
			tag.put("screens", l);
			for (Entry<String, Screen> e : screens.entrySet()) {
				CompoundTag t = new CompoundTag();
				t.putString("name", e.getKey());
				t.putString("id", e.getValue().getID());
				e.getValue().writeTo(t);
				l.add(t);
			}
			return tag;
		}

	}
	public class TranscoderReceiver implements Runnable {
		private Socket s;
		private DataOutputStream dout;
		public TranscoderReceiver(Socket s) {
			this.s = s;
		}

		//TODO better auth
		@Override
		public void run() {
			try {
				DataInputStream is = new DataInputStream(s.getInputStream());
				dout = new DataOutputStream(s.getOutputStream());
				short len = is.readShort();
				if(len < 1)throw new IOException("Invalid string len");
				byte[] nm = new byte[len];
				is.readFully(nm);
				Screen sc = screens.get(new String(nm));
				if(sc == null) {
					dout.writeByte(0);
					throw new IOException("Screen not found");
				}
				dout.writeByte(1);
				try {
					while(true) {
						int w = is.readInt();
						int h = is.readInt();
						byte[] dt = new byte[w*h];
						is.readFully(dt);
						mainExec.accept(() -> {
							try {
								sc.data(dt, w);
							} catch (Exception e) {
								e.printStackTrace();
								try {
									s.close();
								} catch (IOException e1) {
								}
							}
						});
					}
				} finally {
					sc.clear();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void close() {
			try {
				s.close();
			} catch (Exception e) {
			}
		}

	}

	private Map<String, Screen> screens = new HashMap<>();
	private Set<TranscoderReceiver> receivers = new HashSet<>();
	private int port;
	private ExecutorService exec;
	private Consumer<Runnable> mainExec;
	private ServerSocket serverSocket;
	public ScreenManager(int port, Consumer<Runnable> mainExec) {
		super("Screen Manager Thread");
		this.port = port;
		this.mainExec = mainExec;
	}

	@Override
	public void run() {
		exec = Executors.newCachedThreadPool(new ThreadFactory() {
			private final ThreadGroup group;
			private final AtomicInteger threadNumber = new AtomicInteger(1);
			private final String namePrefix;
			{
				SecurityManager s = System.getSecurityManager();
				group = (s != null) ? s.getThreadGroup() :
					Thread.currentThread().getThreadGroup();
				namePrefix = "Screen Manager Pool " +
						poolNumber.getAndIncrement() +
						" Thread ";
			}
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(group, r,
						namePrefix + threadNumber.getAndIncrement(),
						0);
				if (t.isDaemon())
					t.setDaemon(false);
				if (t.getPriority() != Thread.NORM_PRIORITY)
					t.setPriority(Thread.NORM_PRIORITY);
				return t;
			}
		});
		try (ServerSocket ss = new ServerSocket(port)){
			serverSocket = ss;
			while(true) {
				Socket s = ss.accept();
				TranscoderReceiver r = new TranscoderReceiver(s);
				receivers.add(r);
				exec.execute(r);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			exec.shutdown();
		}
	}

	public void close() {
		try {
			serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		receivers.forEach(TranscoderReceiver::close);
	}

	public void addScreen(String string, Screen mapScreen) {
		screens.put(string, mapScreen);
		state.markDirty();
	}
}
