package net.imglib2.cache.python.examples;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import jep.JepException;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.python.Halo;
import net.imglib2.cache.python.PythonCacheLoader;
import net.imglib2.cache.python.PythonCacheLoaderQueue;
import net.imglib2.img.basictypeaccess.nio.BufferAccess;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.numeric.integer.LongType;

/**
 * Example for making predictions using stardist: https://github.com/stardist/stardist
 * Requires installation of Python with numpy, jep, tensorflow, and stardist packages installed.
 * It may be necessary to set PYTHONHOME appropriately.
 */
public class StarDist {
	public static void main(String... args) throws InterruptedException, JepException {
		final String init = String.join(
				"\n",
				"from stardist.data import test_image_nuclei_2d",
				"from stardist.models import StarDist2D",
				"from stardist.plot import render_label",
				"from csbdeep.utils import normalize",
				"img = test_image_nuclei_2d()",
				"model = StarDist2D.from_pretrained('2D_versatile_fluo')"
		);
		final String code = String.join(
				"\n",
				"halo = 10",
				"offsets = tuple(min(m, halo) for m in block.min)",
				"print(f'{offsets=}')",
				"slicing = tuple(slice(m - o, M+1 + halo) for o, m, M in zip(offsets, block.min, block.max))",
				"# slicing = tuple(slice(m, M+1) for m, M in zip(block.min, block.max))",
				"labels, _ = model.predict_instances(normalize(img[slicing]))",
				"block.data[...] = labels[tuple(slice(o, o + s) for o, s in zip(offsets, block.data.shape))] # labels"
		);
		final long[] dims = {512, 512};
		final int[] bs = {80, 90};
		final CellGrid grid = new CellGrid(dims, bs);
		final PythonCacheLoader<LongType, ? extends BufferAccess<?>> loader = PythonCacheLoader.fromRandomAccessibles(
				grid,
				new PythonCacheLoaderQueue(3, init),
				code,
				new LongType(),
				Halo.empty(2));
		final CachedCellImg<LongType, ? extends BufferAccess<?>> img = loader.createCachedCellImg(30);

		final BdvStackSource<?> bdv = BdvFunctions.show(
				VolatileViews.wrapAsVolatile(img, new SharedQueue(10, 1)),
				"stardist",
				BdvOptions.options().numRenderingThreads(10).is2D());
		bdv.setDisplayRange(0.0, 8.0);
	}
}
