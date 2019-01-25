package bc19;

public class node
{


    node parentNode;


    node[] successors = new node[8]; //place to store the successor nodes around this node


    //x and y are this node's corrdinates
    int x = 0;
    int y = 0;



    //these values are used in A* algorithm
    int g = 0;
    int k = 0;
    int f = 0;
}