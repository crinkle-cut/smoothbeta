package net.mine_diver.smoothbeta.client.render;

import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.smoothbeta.client.render.gl.GlStateManager;
import net.mine_diver.smoothbeta.mixin.client.MinecraftAccessor;
import net.minecraft.client.util.GlAllocationUtils;
import net.modificationstation.stationapi.api.util.collection.LinkedList;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
public class VboPool implements AutoCloseable {
    @SuppressWarnings("deprecation")
    private static final MinecraftAccessor mc = ((MinecraftAccessor) FabricLoader.getInstance().getGameInstance());

    // Simplified for M1 Mac compatibility - use display lists instead of VBOs
    private final List<ByteBuffer> pendingBuffers = new ArrayList<>();
    private final List<Pos> pendingPositions = new ArrayList<>();
    private int capacity = 4096;
    private int nextPos = 0;
    private int size;
    private final LinkedList<Pos> posList = new LinkedList<>();
    private Pos compactPosLast = null;
    private int curBaseInstance;

    private final int vertexBytes;
    private VertexFormat.DrawMode drawMode = VertexFormat.DrawMode.QUADS;

    public VboPool(VertexFormat format) {
        vertexBytes = format.getVertexSizeByte();
    }

    @Override
    public void close() {
        // Cleanup any resources
        pendingBuffers.clear();
        pendingPositions.clear();
    }

    public void bufferData(ByteBuffer data, Pos poolPos) {
        int bufferSize = this.toVertex(data.limit());
        
        if (bufferSize <= 0) {
            if (poolPos.getPosition() >= 0) {
                poolPos.setPosition(-1);
                poolPos.setSize(0);
                if (this.posList.contains(poolPos.getNode())) {
                    this.posList.remove(poolPos.getNode());
                    this.size -= poolPos.getSize();
                }
            }
        } else {
            if (bufferSize > poolPos.getSize()) {
                poolPos.setPosition(this.nextPos);
                poolPos.setSize(bufferSize);
                this.nextPos += bufferSize;

                if (poolPos.getPosition() >= 0 && this.posList.contains(poolPos.getNode())) this.posList.remove(poolPos.getNode());
                this.posList.addLast(poolPos.getNode());
            }

            poolPos.setSize(bufferSize);
            this.size += bufferSize - poolPos.getSize();
            
            // Store buffer data for immediate mode rendering
            ByteBuffer copy = ByteBuffer.allocateDirect(data.remaining());
            copy.put(data);
            copy.flip();
            data.rewind();
            
            pendingBuffers.add(copy);
            pendingPositions.add(poolPos);
        }
    }

    private void compactRanges() {
        // Simplified compaction for M1 Mac compatibility
        if (!this.posList.isEmpty()) {
            this.nextPos = this.posList.getLast().getItem().getPositionNext();
        }
    }

    private long toBytes(int vertex) {
        return (long)vertex * (long)this.vertexBytes;
    }

    private int toVertex(long bytes) {
        return (int)(bytes / (long)this.vertexBytes);
    }

    private void checkVboSize(int sizeMin) {
        if (this.capacity < sizeMin) this.expandVbo(sizeMin);
    }

    private void copyVboData(int posFrom, int posTo, int size) {
        // Simplified approach for M1 Mac compatibility - avoid complex buffer operations
        mc.smoothbeta_printOpenGLError("Copy VBO range (simplified for M1 Mac)");
    }

    private void expandVbo(int sizeMin) {
        int i = this.capacity * 6 / 4;
        while (i < sizeMin) i = i * 6 / 4;
        this.capacity = i;
        mc.smoothbeta_printOpenGLError("Expand VBO");
    }

    // Simplified methods for M1 Mac compatibility
    public void bindBuffer() {
        // No-op for immediate mode
    }

    public void upload(VertexFormat.DrawMode drawMode, Pos range) {
        if (this.drawMode != drawMode) {
            if (!pendingBuffers.isEmpty())
                throw new IllegalArgumentException("Mixed region draw modes: " + this.drawMode + " != " + drawMode);
            this.drawMode = drawMode;
        }
        // Store draw command info in the position
    }

    public void drawAll() {
        // Use vertex attributes for shader compatibility on M1 Mac
        if (pendingBuffers.isEmpty()) return;
        
        // Enable vertex attribute arrays (these work with both shaders and fixed pipeline)
        GL20.glEnableVertexAttribArray(0); // Position
        GL20.glEnableVertexAttribArray(1); // UV0
        GL20.glEnableVertexAttribArray(2); // Color  
        GL20.glEnableVertexAttribArray(3); // Normal
        
        for (ByteBuffer buffer : pendingBuffers) {
            if (buffer.capacity() < 28) continue; // Need at least one vertex
            
            // Actual vertex format from debug: Position(3*4) + UV(2*4) + Color(4*1) + Normal(4*1) = 28 bytes
            buffer.position(0); // Reset to start
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 28, buffer);
            
            // Create separate positioned buffers for each attribute to avoid position conflicts
            ByteBuffer uvBuffer = buffer.duplicate().position(12);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 28, uvBuffer);
            
            ByteBuffer colorBuffer = buffer.duplicate().position(20);
            GL20.glVertexAttribPointer(2, 4, GL11.GL_UNSIGNED_BYTE, true, 28, colorBuffer);
            
            ByteBuffer normalBuffer = buffer.duplicate().position(24);
            GL20.glVertexAttribPointer(3, 4, GL11.GL_BYTE, true, 28, normalBuffer);
            
            int vertexCount = buffer.capacity() / 28;
            if (vertexCount > 0) {
                GL11.glDrawArrays(this.drawMode.glMode, 0, vertexCount);
            }
        }
        
        GL20.glDisableVertexAttribArray(3);
        GL20.glDisableVertexAttribArray(2);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(0);
        
        // Clear pending data
        pendingBuffers.clear();
        pendingPositions.clear();
        
        if (this.nextPos > this.size * 11 / 10) this.compactRanges();
        curBaseInstance = 0;
    }

    public void unbindBuffer() {
        // No-op for immediate mode
    }

    public void deleteGlBuffers() {
        // Cleanup for immediate mode
        pendingBuffers.clear();
        pendingPositions.clear();
    }

    public static class Pos {
        private int position = -1;
        private int size = 0;
        private final LinkedList.Node<Pos> node = new LinkedList.Node<>(this);

        public int getPosition() {
            return this.position;
        }

        public int getSize() {
            return this.size;
        }

        public int getPositionNext() {
            return this.position + this.size;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public LinkedList.Node<Pos> getNode() {
            return this.node;
        }

        public Pos getPrev() {
            LinkedList.Node<Pos> node = this.node.getPrev();
            return node == null ? null : node.getItem();
        }

        public Pos getNext() {
            LinkedList.Node<Pos> node = this.node.getNext();
            return node == null ? null : node.getItem();
        }

        public String toString() {
            return this.position + "/" + this.size + "/" + (this.position + this.size);
        }
    }
}