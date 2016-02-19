package org.to2mbn.jmccc.mcdownloader.provider.liteloader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.to2mbn.jmccc.mcdownloader.RemoteVersionList;
import org.to2mbn.jmccc.mcdownloader.download.DownloadCallback;
import org.to2mbn.jmccc.mcdownloader.download.DownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.MemoryDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.ResultProcessor;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadCallback;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadContext;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.provider.InstallProfileProcessor;
import org.to2mbn.jmccc.mcdownloader.provider.MinecraftDownloadProvider;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.version.Asset;
import org.to2mbn.jmccc.version.Library;
import org.to2mbn.jmccc.version.Version;

public class LiteloaderDownloadProvider implements MinecraftDownloadProvider {

	private static final Pattern LITELOADER_VERSION_PATTERN = Pattern.compile("^([\\w\\.\\-]+)-LiteLoader[\\w\\.\\-]+$");

	public CombinedDownloadTask<LiteloaderVersionList> liteloaderVersionList() {
		try {
			return CombinedDownloadTask.single(new MemoryDownloadTask(new URI("http://dl.liteloader.com/versions/versions.json")).andThen(new ResultProcessor<byte[], LiteloaderVersionList>() {

				@Override
				public LiteloaderVersionList process(byte[] arg) throws Exception {
					return LiteloaderVersionList.fromJson(new JSONObject(new String(arg, "UTF-8")));
				}
			}));
		} catch (URISyntaxException e) {
			throw new IllegalStateException("unable to convert to URI", e);
		}
	}

	@Override
	public CombinedDownloadTask<Object> gameVersionJson(final MinecraftDirectory mcdir, final String version) {
		if (!LITELOADER_VERSION_PATTERN.matcher(version).matches()) {
			return null;
		}
		final String mcversion = version.substring(0, version.indexOf("-LiteLoader"));
		return new CombinedDownloadTask<Object>() {

			@Override
			public void execute(final CombinedDownloadContext<Object> context) throws Exception {
				context.submit(liteloaderVersionList(), new CombinedDownloadCallback<LiteloaderVersionList>() {

					@Override
					public void done(final LiteloaderVersionList versionList) {
						try {
							context.submit(new Callable<Object>() {

								@Override
								public Object call() throws Exception {
									LiteloaderVersion liteloaderVersion = versionList.getLatestArtefact(mcversion);
									context.submit(new MemoryDownloadTask(new URI("http://dl.liteloader.com/redist/" + mcversion + "/liteloader-installer-" + liteloaderVersion.getLiteloaderVersion().replace('_', '-') + ".jar")).andThen(new InstallProfileProcessor(mcdir.getVersionJson(version))), null, true);
									context.awaitAllTasks(new Runnable() {

										@Override
										public void run() {
											context.done(null);
										}
									});
									return null;
								}
							}, null, true);
						} catch (InterruptedException e) {
							context.cancelled();
						}
					}

					@Override
					public void failed(Throwable e) {
					}

					@Override
					public void cancelled() {
					}

					@Override
					public <R> DownloadCallback<R> taskStart(DownloadTask<R> task) {
						return null;
					}

				}, true);
			}
		};
	}

	@Override
	public CombinedDownloadTask<RemoteVersionList> versionList() {
		return null;
	}

	@Override
	public CombinedDownloadTask<Set<Asset>> assetsIndex(MinecraftDirectory mcdir, Version version) {
		return null;
	}

	@Override
	public CombinedDownloadTask<Object> gameJar(MinecraftDirectory mcdir, Version version) {
		return null;
	}

	@Override
	public CombinedDownloadTask<Object> library(MinecraftDirectory mcdir, Library library) {
		return null;
	}

	@Override
	public CombinedDownloadTask<Object> asset(MinecraftDirectory mcdir, Asset asset) {
		return null;
	}

}
