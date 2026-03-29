package tage.shapes;
import tage.*;

/**
 * A Pyramid with 18 vertices (6 triangles):
 * - 4 side faces (triangles)
 * - 1 base (2 triangles)
 *
 * Apex is at y=1, base is at y=-1.
 */
public class Pyramid extends ObjShape
{
    private float[] vertices = new float[]
    {

        -1.0f, -1.0f,  1.0f, 1.0f, -1.0f,  1.0f,0.0f,  1.0f,  0.0f, // Side 1
         1.0f, -1.0f,  1.0f,1.0f, -1.0f, -1.0f, 0.0f,  1.0f,  0.0f, // Side 2
         1.0f, -1.0f, -1.0f,-1.0f, -1.0f, -1.0f, 0.0f,  1.0f,  0.0f, // Side 3
		-1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f, 0.0f,  1.0f,  0.0f, // Side 4

        -1.0f, -1.0f,  1.0f, 1.0f, -1.0f,  1.0f, 1.0f, -1.0f, -1.0f, // Base
		-1.0f, -1.0f,  1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f
    };

    // Simple per-face UVs (each triangle gets (0,0)-(1,0)-(0.5,1) style)
    private float[] texCoords = new float[]
    {
        0.0f, 0.0f,   1.0f, 0.0f,   0.5f, 1.0f,
        0.0f, 0.0f,   1.0f, 0.0f,   0.5f, 1.0f,
        0.0f, 0.0f,   1.0f, 0.0f,   0.5f, 1.0f,
        0.0f, 0.0f,   1.0f, 0.0f,   0.5f, 1.0f,
        0.0f, 0.0f,   1.0f, 0.0f,   1.0f, 1.0f,
        0.0f, 0.0f,   1.0f, 1.0f,   0.0f, 1.0f
    };
	
private float[] normals = new float[]
{
    0.0f, 0.5f, 1.0f,   0.0f, 0.5f, 1.0f,   0.0f, 0.5f, 1.0f,
    1.0f, 0.5f, 0.0f,   1.0f, 0.5f, 0.0f,   1.0f, 0.5f, 0.0f,
    0.0f, 0.5f, -1.0f,  0.0f, 0.5f, -1.0f,  0.0f, 0.5f, -1.0f,
    -1.0f, 0.5f, 0.0f,  -1.0f, 0.5f, 0.0f,  -1.0f, 0.5f, 0.0f,
    0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,
    0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f
};

    public Pyramid()
    {
        setNumVertices(18);
        setVertices(vertices);
        setTexCoords(texCoords);
        setNormals(normals);
        setWindingOrderCCW(false);
    }
}