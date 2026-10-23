import os
import sys, json
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from svgconv import load
import custom
SRC = {  # id -> (lib, name) or None for custom
 "car":("t","car"),"heavy":("t","truck"),"bus":("t","bus"),"motorcycle":("t","motorbike"),"bicycle":("t","bike"),"train":("t","train"),"plane":("t","plane"),"helicopter":("t","helicopter"),"boat":("t","sailboat"),
 "dog":("t","dog"),"cat":("t","cat"),"bird":("l","bird"),"horse":("t","horse"),"cattle":None,"bear":None,"reptile":("l","turtle"),"insect":("t","bug"),
 "mountain":("t","mountain"),"beach":("t","beach"),"desert":("t","cactus"),"forest":("t","trees"),"water":("t","ripple"),"waterfall":None,"sunset":("t","sunset-2"),"flower":("t","flower"),"ice":("t","snowflake"),"storm":("t","cloud-storm"),
 "building":("t","building"),"skyscraper":("t","building-skyscraper"),"bridge":("t","building-bridge"),"tower":("t","building-eiffel-tower"),"mosque":("t","building-mosque"),"church":("t","building-church"),"castle":("t","building-castle"),"landmark":("t","building-monument"),"road":("t","road"),
 "meal":("t","soup"),"fastfood":("t","burger"),"sweets":("l","cupcake"),"bread":("t","bread"),"fruit":("t","apple"),"vegetable":("t","carrot"),"coffee":("t","coffee"),"drink":("t","glass-cocktail"),
 "soccer":("t","ball-football"),"swimming":("t","swimming"),"running":("t","run"),"dance":None,"concert":("t","microphone-2"),"party":("t","confetti"),"wedding":None,"fireworks":None,"fire":("t","flame"),
 "phone":("t","device-mobile"),"computer":("t","device-laptop"),"tv":("t","device-tv"),"instrument":("t","piano"),"clock":("t","clock"),"bag":("t","shopping-bag"),"glasses":("t","eyeglass"),"toy":("t","lego"),
 "shoes":("t","shoe"),"hat":None,"dress":None,"clothes":("t","shirt"),"jewellery":("t","diamond"),
 "face":("t","user-scan"),"smile":("t","mood-smile"),"group":("t","users-group"),"baby":("t","baby-carriage"),"crowd":("t","users"),"text":("t","text-recognition"),"code":("t","qrcode"),
}
GROUP_SRC = {"vehicles":("t","car"),"animals":("t","paw"),"nature":("t","mountain"),"places":("t","buildings"),"food":("t","tools-kitchen-2"),
 "activities":("t","trophy"),"devices":("t","device-mobile"),"fashion":("t","shirt"),"people":("t","mood-smile")}
ICONS={}
for k,v in SRC.items():
    from svgconv import normalize
    ICONS[k]=[normalize(p) for p in (custom.C[k] if v is None else load(*v))]

GROUP_ICONS = {k: [normalize(p) for p in load(*v)] for k, v in GROUP_SRC.items()}
