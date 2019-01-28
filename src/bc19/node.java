package bc19;

public class node
{
    node parentNode;

    //x and y are this node's corrdinates
    public int x;
    public int y;

    public node(int posX, int posY, node parent) {
        x = posX;
        y = posY;
        parentNode = parent;
    }
}