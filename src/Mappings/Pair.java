package Mappings;

import java.util.ArrayList;

public class Pair<F,S>
{
    private F first;
    private S second;
    public void setPair(F f, S s)
    {
        this.first = f;
        this.second = s;
    }
    Pair(){}
    Pair(F f, S s)
    {
        this.first = f;
        this.second = s;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }
}