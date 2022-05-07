package Mappings;

import java.util.ArrayList;

public class Type{
    //This class is designed to store type of the ASTNode
    String name;
    private static ArrayList<Type> types = new ArrayList<>();
    public Type(String name)
    {
        this.name = name;
        types.add(this);
    }
}