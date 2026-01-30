package com.dark98.santoku.slic3r;

import java.util.Arrays;
import java.util.List;

class OCCTLoader {
    private final static List<String> LIBS = Arrays.asList(
            "TKDESTEP",
            "TKXCAF",
            "TKLCAF",
            "TKCAF",
            "TKCDF",
            "TKV3d",
            "TKMesh",
            "TKXMesh",
            "TKBO",
            "TKPrim",
            "TKHLR",
            "TKShHealing",
            "TKTopAlgo",
            "TKGeomAlgo",
            "TKGeomBase",
            "TKBRep",
            "TKG3d",
            "TKG2d",
            "TKMath",
            "TKernel",
            "TKDE"
    );

    static void load() {
        for (String lib : LIBS) {
            System.loadLibrary(lib);
        }
    }
}
