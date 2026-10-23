import math
from svgconv import f, circle
def pt(cx,cy,r,deg): 
    a=math.radians(deg); return cx+r*math.cos(a), cy+r*math.sin(a)
def dot(x,y): return f"M{f(x)} {f(y)}h.01"
C={}
# Bear: head circle with two ear arcs
def bear():
    cx,cy,r=12,13.2,6.6
    p1=pt(cx,cy,r,200); p2=pt(cx,cy,r,245)   # left ear on circle
    q1=pt(cx,cy,r,295); q2=pt(cx,cy,r,340)
    # head arc from p2 around bottom to q1 (long way through bottom)
    head=f"M{f(p2[0])} {f(p2[1])}A{r} {r} 0 0 0 {f(p1[0])} {f(p1[1])}A{r} {r} 0 1 0 {f(q2[0])} {f(q2[1])}A{r} {r} 0 0 0 {f(q1[0])} {f(q1[1])}"
    # ears arcs outside (bulge outward), radius 2.4
    ear1=f"M{f(p1[0])} {f(p1[1])}A2.6 2.6 0 0 1 {f(p2[0])} {f(p2[1])}"
    ear2=f"M{f(q1[0])} {f(q1[1])}A2.6 2.6 0 0 1 {f(q2[0])} {f(q2[1])}"
    return [head,ear1,ear2,dot(9.5,11.6),dot(14.5,11.6),circle(12,15.6,2.2),dot(12,14.9)]
C["bear"]=bear()
C["cattle"]=[
 "M7 6h10l2.2 3.6v6.2a4 4 0 0 1-4 4H8.8a4 4 0 0 1-4-4V9.6z",
 "M7 6 5.2 3.4M17 6l1.8-2.6",
 "M4.8 10.2 2.2 9.4c-.4 2.4.6 3.8 2.6 4.2M19.2 10.2l2.6-.8c.4 2.4-.6 3.8-2.6 4.2",
 dot(9.2,11),dot(14.8,11),
 "M8.2 15.2h7.6v1.2a2.4 2.4 0 0 1-2.4 2.4h-2.8a2.4 2.4 0 0 1-2.4-2.4z",
 dot(10.6,17),dot(13.4,17)]
C["waterfall"]=[
 "M2.5 4h4v9l-4 3.5M21.5 4h-4v9l4 3.5",
 "M9.5 4v9M12 4v12M14.5 4v9",
 "M3 20.6c1.4-1.4 2.8-1.4 4.2 0s2.8 1.4 4.2 0 2.8-1.4 4.2 0 2.8 1.4 4.2 0"]
C["fireworks"]=[
 "M12 8v2.5M12 15.5V18M6 13h2.5M15.5 13H18M7.8 8.8l1.8 1.8M14.4 15.4l1.8 1.8M16.2 8.8l-1.8 1.8M9.6 15.4l-1.8 1.8",
 dot(12,4.5),dot(5.2,13),dot(18.8,13),dot(12,21.5),dot(7,6.5),dot(17,6.5),dot(7,19.5),dot(17,19.5)]
C["dance"]=[
 circle(14.5,4.6,1.7),
 "M13.4 8.6 11.6 14",
 "M13.2 9.2 18.2 6.6M12.9 9.6 8.4 12",
 "M11.6 14l3.6 3.4-.9 3.6M11.6 14 8 17.2l-2.1 1.2"]
C["hat"]=[
 "M7.2 14V10a4.8 4.8 0 0 1 9.6 0v4",
 "M7.2 11.8h9.6",
 "M7.2 14C4.2 14.4 2.5 15.4 2.5 16.6c0 1.9 4.2 3.2 9.5 3.2s9.5-1.3 9.5-3.2c0-1.2-1.7-2.2-4.7-2.6"]
C["dress"]=[
 "M9.2 3.2 12 7.4l2.8-4.2",
 "M9.2 3.2 8.5 10.6 3.8 21h16.4l-4.7-10.4-.7-7.4",
 "M8.5 10.6c2.4 1 4.6 1 7 0"]
C["wedding"]=[
 circle(8.4,15.4,4.4),circle(15.6,15.4,4.4),
 "M12 2.6l2.4 2.6-2.4 2.9-2.4-2.9z"]
