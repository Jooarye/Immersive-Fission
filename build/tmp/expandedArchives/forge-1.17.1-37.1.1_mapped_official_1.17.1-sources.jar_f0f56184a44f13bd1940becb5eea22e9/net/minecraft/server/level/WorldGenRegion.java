package net.minecraft.server.level;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldGenRegion implements WorldGenLevel {
   private static final Logger LOGGER = LogManager.getLogger();
   private final List<ChunkAccess> cache;
   private final ChunkPos center;
   private final int size;
   private final ServerLevel level;
   private final long seed;
   private final LevelData levelData;
   private final Random random;
   private final DimensionType dimensionType;
   private final TickList<Block> blockTicks = new WorldGenTickList<>((p_9597_) -> {
      return this.getChunk(p_9597_).getBlockTicks();
   });
   private final TickList<Fluid> liquidTicks = new WorldGenTickList<>((p_9595_) -> {
      return this.getChunk(p_9595_).getLiquidTicks();
   });
   private final BiomeManager biomeManager;
   private final ChunkPos firstPos;
   private final ChunkPos lastPos;
   private final StructureFeatureManager structureFeatureManager;
   private final ChunkStatus generatingStatus;
   private final int writeRadiusCutoff;
   @Nullable
   private Supplier<String> currentlyGenerating;

   public WorldGenRegion(ServerLevel p_143484_, List<ChunkAccess> p_143485_, ChunkStatus p_143486_, int p_143487_) {
      this.generatingStatus = p_143486_;
      this.writeRadiusCutoff = p_143487_;
      int i = Mth.floor(Math.sqrt((double)p_143485_.size()));
      if (i * i != p_143485_.size()) {
         throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Cache size is not a square."));
      } else {
         ChunkPos chunkpos = p_143485_.get(p_143485_.size() / 2).getPos();
         this.cache = p_143485_;
         this.center = chunkpos;
         this.size = i;
         this.level = p_143484_;
         this.seed = p_143484_.getSeed();
         this.levelData = p_143484_.getLevelData();
         this.random = p_143484_.getRandom();
         this.dimensionType = p_143484_.dimensionType();
         this.biomeManager = new BiomeManager(this, BiomeManager.obfuscateSeed(this.seed), p_143484_.dimensionType().getBiomeZoomer());
         this.firstPos = p_143485_.get(0).getPos();
         this.lastPos = p_143485_.get(p_143485_.size() - 1).getPos();
         this.structureFeatureManager = p_143484_.structureFeatureManager().forWorldGenRegion(this);
      }
   }

   public ChunkPos getCenter() {
      return this.center;
   }

   public void setCurrentlyGenerating(@Nullable Supplier<String> p_143498_) {
      this.currentlyGenerating = p_143498_;
   }

   public ChunkAccess getChunk(int p_9507_, int p_9508_) {
      return this.getChunk(p_9507_, p_9508_, ChunkStatus.EMPTY);
   }

   @Nullable
   public ChunkAccess getChunk(int p_9514_, int p_9515_, ChunkStatus p_9516_, boolean p_9517_) {
      ChunkAccess chunkaccess;
      if (this.hasChunk(p_9514_, p_9515_)) {
         int i = p_9514_ - this.firstPos.x;
         int j = p_9515_ - this.firstPos.z;
         chunkaccess = this.cache.get(i + j * this.size);
         if (chunkaccess.getStatus().isOrAfter(p_9516_)) {
            return chunkaccess;
         }
      } else {
         chunkaccess = null;
      }

      if (!p_9517_) {
         return null;
      } else {
         LOGGER.error("Requested chunk : {} {}", p_9514_, p_9515_);
         LOGGER.error("Region bounds : {} {} | {} {}", this.firstPos.x, this.firstPos.z, this.lastPos.x, this.lastPos.z);
         if (chunkaccess != null) {
            throw (RuntimeException)Util.pauseInIde(new RuntimeException(String.format("Chunk is not of correct status. Expecting %s, got %s | %s %s", p_9516_, chunkaccess.getStatus(), p_9514_, p_9515_)));
         } else {
            throw (RuntimeException)Util.pauseInIde(new RuntimeException(String.format("We are asking a region for a chunk out of bound | %s %s", p_9514_, p_9515_)));
         }
      }
   }

   public boolean hasChunk(int p_9574_, int p_9575_) {
      return p_9574_ >= this.firstPos.x && p_9574_ <= this.lastPos.x && p_9575_ >= this.firstPos.z && p_9575_ <= this.lastPos.z;
   }

   public BlockState getBlockState(BlockPos p_9587_) {
      return this.getChunk(SectionPos.blockToSectionCoord(p_9587_.getX()), SectionPos.blockToSectionCoord(p_9587_.getZ())).getBlockState(p_9587_);
   }

   public FluidState getFluidState(BlockPos p_9577_) {
      return this.getChunk(p_9577_).getFluidState(p_9577_);
   }

   @Nullable
   public Player getNearestPlayer(double p_9501_, double p_9502_, double p_9503_, double p_9504_, Predicate<Entity> p_9505_) {
      return null;
   }

   public int getSkyDarken() {
      return 0;
   }

   public BiomeManager getBiomeManager() {
      return this.biomeManager;
   }

   public Biome getUncachedNoiseBiome(int p_9510_, int p_9511_, int p_9512_) {
      return this.level.getUncachedNoiseBiome(p_9510_, p_9511_, p_9512_);
   }

   public float getShade(Direction p_9555_, boolean p_9556_) {
      return 1.0F;
   }

   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   public boolean destroyBlock(BlockPos p_9550_, boolean p_9551_, @Nullable Entity p_9552_, int p_9553_) {
      BlockState blockstate = this.getBlockState(p_9550_);
      if (blockstate.isAir()) {
         return false;
      } else {
         if (p_9551_) {
            BlockEntity blockentity = blockstate.hasBlockEntity() ? this.getBlockEntity(p_9550_) : null;
            Block.dropResources(blockstate, this.level, p_9550_, blockentity, p_9552_, ItemStack.EMPTY);
         }

         return this.setBlock(p_9550_, Blocks.AIR.defaultBlockState(), 3, p_9553_);
      }
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos p_9582_) {
      ChunkAccess chunkaccess = this.getChunk(p_9582_);
      BlockEntity blockentity = chunkaccess.getBlockEntity(p_9582_);
      if (blockentity != null) {
         return blockentity;
      } else {
         CompoundTag compoundtag = chunkaccess.getBlockEntityNbt(p_9582_);
         BlockState blockstate = chunkaccess.getBlockState(p_9582_);
         if (compoundtag != null) {
            if ("DUMMY".equals(compoundtag.getString("id"))) {
               if (!blockstate.hasBlockEntity()) {
                  return null;
               }

               blockentity = ((EntityBlock)blockstate.getBlock()).newBlockEntity(p_9582_, blockstate);
            } else {
               blockentity = BlockEntity.loadStatic(p_9582_, blockstate, compoundtag);
            }

            if (blockentity != null) {
               chunkaccess.setBlockEntity(blockentity);
               return blockentity;
            }
         }

         if (blockstate.hasBlockEntity()) {
            LOGGER.warn("Tried to access a block entity before it was created. {}", (Object)p_9582_);
         }

         return null;
      }
   }

   public boolean ensureCanWrite(BlockPos p_181031_) {
      int i = SectionPos.blockToSectionCoord(p_181031_.getX());
      int j = SectionPos.blockToSectionCoord(p_181031_.getZ());
      int k = Math.abs(this.center.x - i);
      int l = Math.abs(this.center.z - j);
      if (k <= this.writeRadiusCutoff && l <= this.writeRadiusCutoff) {
         return true;
      } else {
         Util.logAndPauseIfInIde("Detected setBlock in a far chunk [" + i + ", " + j + "], pos: " + p_181031_ + ", status: " + this.generatingStatus + (this.currentlyGenerating == null ? "" : ", currently generating: " + (String)this.currentlyGenerating.get()));
         return false;
      }
   }

   public boolean setBlock(BlockPos p_9539_, BlockState p_9540_, int p_9541_, int p_9542_) {
      if (!this.ensureCanWrite(p_9539_)) {
         return false;
      } else {
         ChunkAccess chunkaccess = this.getChunk(p_9539_);
         BlockState blockstate = chunkaccess.setBlockState(p_9539_, p_9540_, false);
         if (blockstate != null) {
            this.level.onBlockStateChange(p_9539_, blockstate, p_9540_);
         }

         if (p_9540_.hasBlockEntity()) {
            if (chunkaccess.getStatus().getChunkType() == ChunkStatus.ChunkType.LEVELCHUNK) {
               BlockEntity blockentity = ((EntityBlock)p_9540_.getBlock()).newBlockEntity(p_9539_, p_9540_);
               if (blockentity != null) {
                  chunkaccess.setBlockEntity(blockentity);
               } else {
                  chunkaccess.removeBlockEntity(p_9539_);
               }
            } else {
               CompoundTag compoundtag = new CompoundTag();
               compoundtag.putInt("x", p_9539_.getX());
               compoundtag.putInt("y", p_9539_.getY());
               compoundtag.putInt("z", p_9539_.getZ());
               compoundtag.putString("id", "DUMMY");
               chunkaccess.setBlockEntityNbt(compoundtag);
            }
         } else if (blockstate != null && blockstate.hasBlockEntity()) {
            chunkaccess.removeBlockEntity(p_9539_);
         }

         if (p_9540_.hasPostProcess(this, p_9539_)) {
            this.markPosForPostprocessing(p_9539_);
         }

         return true;
      }
   }

   private void markPosForPostprocessing(BlockPos p_9592_) {
      this.getChunk(p_9592_).markPosForPostprocessing(p_9592_);
   }

   public boolean addFreshEntity(Entity p_9580_) {
      int i = SectionPos.blockToSectionCoord(p_9580_.getBlockX());
      int j = SectionPos.blockToSectionCoord(p_9580_.getBlockZ());
      this.getChunk(i, j).addEntity(p_9580_);
      return true;
   }

   public boolean removeBlock(BlockPos p_9547_, boolean p_9548_) {
      return this.setBlock(p_9547_, Blocks.AIR.defaultBlockState(), 3);
   }

   public WorldBorder getWorldBorder() {
      return this.level.getWorldBorder();
   }

   public boolean isClientSide() {
      return false;
   }

   @Deprecated
   public ServerLevel getLevel() {
      return this.level;
   }

   public RegistryAccess registryAccess() {
      return this.level.registryAccess();
   }

   public LevelData getLevelData() {
      return this.levelData;
   }

   public DifficultyInstance getCurrentDifficultyAt(BlockPos p_9585_) {
      if (!this.hasChunk(SectionPos.blockToSectionCoord(p_9585_.getX()), SectionPos.blockToSectionCoord(p_9585_.getZ()))) {
         throw new RuntimeException("We are asking a region for a chunk out of bound");
      } else {
         return new DifficultyInstance(this.level.getDifficulty(), this.level.getDayTime(), 0L, this.level.getMoonBrightness());
      }
   }

   @Nullable
   public MinecraftServer getServer() {
      return this.level.getServer();
   }

   public ChunkSource getChunkSource() {
      return this.level.getChunkSource();
   }

   public long getSeed() {
      return this.seed;
   }

   public TickList<Block> getBlockTicks() {
      return this.blockTicks;
   }

   public TickList<Fluid> getLiquidTicks() {
      return this.liquidTicks;
   }

   public int getSeaLevel() {
      return this.level.getSeaLevel();
   }

   public Random getRandom() {
      return this.random;
   }

   public int getHeight(Heightmap.Types p_9535_, int p_9536_, int p_9537_) {
      return this.getChunk(SectionPos.blockToSectionCoord(p_9536_), SectionPos.blockToSectionCoord(p_9537_)).getHeight(p_9535_, p_9536_ & 15, p_9537_ & 15) + 1;
   }

   public void playSound(@Nullable Player p_9528_, BlockPos p_9529_, SoundEvent p_9530_, SoundSource p_9531_, float p_9532_, float p_9533_) {
   }

   public void addParticle(ParticleOptions p_9561_, double p_9562_, double p_9563_, double p_9564_, double p_9565_, double p_9566_, double p_9567_) {
   }

   public void levelEvent(@Nullable Player p_9523_, int p_9524_, BlockPos p_9525_, int p_9526_) {
   }

   public void gameEvent(@Nullable Entity p_143490_, GameEvent p_143491_, BlockPos p_143492_) {
   }

   public DimensionType dimensionType() {
      return this.dimensionType;
   }

   public boolean isStateAtPosition(BlockPos p_9544_, Predicate<BlockState> p_9545_) {
      return p_9545_.test(this.getBlockState(p_9544_));
   }

   public boolean isFluidAtPosition(BlockPos p_143500_, Predicate<FluidState> p_143501_) {
      return p_143501_.test(this.getFluidState(p_143500_));
   }

   public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> p_143494_, AABB p_143495_, Predicate<? super T> p_143496_) {
      return Collections.emptyList();
   }

   public List<Entity> getEntities(@Nullable Entity p_9519_, AABB p_9520_, @Nullable Predicate<? super Entity> p_9521_) {
      return Collections.emptyList();
   }

   public List<Player> players() {
      return Collections.emptyList();
   }

   public Stream<? extends StructureStart<?>> startsForFeature(SectionPos p_9558_, StructureFeature<?> p_9559_) {
      return this.structureFeatureManager.startsForFeature(p_9558_, p_9559_);
   }

   public int getMinBuildHeight() {
      return this.level.getMinBuildHeight();
   }

   public int getHeight() {
      return this.level.getHeight();
   }
}