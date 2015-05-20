// set R2 to zero 
@2
M=0

// if R0 or R1 are zero return 
@0
D=M
@END
D;JEQ
@1
D=M
@END
D;JEQ

//====================================
// R3 = R1 - R0 
@0
D=M
@3
M=D
@1
D=M
@3
M=D-M
// if (R3 < 0) skip the swap and GOTO MULT
D=M
@MULT
D;JLT
//====================================
(SWAP)
@3
D=M
@0
M=M+D
@1
M=M-D

(MULT)
//  R3 = R1
@1
D=M
@3
M=D
(LOOP)

// R2 = R2 + R0
@0
D=M
@2
M=M+D

// R3 = R3 - 1
@3
M=M-1
D=M
@END
D;JEQ
@LOOP
0;JEQ


(END)
@END
0;JEQ



