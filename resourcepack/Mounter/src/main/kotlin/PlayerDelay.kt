package kr.kro.lanthanide

class PlayerDelay(
    var head: Int = 0,
    var rightArm: Int = 0,
    var leftArm: Int = 0,
    var body: Int = 0,
    var leftLeg: Int = 0,
    var rightLeg: Int = 0
) {
    
    fun resetAll() {
        head = 0
        rightArm = 0
        leftArm = 0
        body = 0
        leftLeg = 0
        rightLeg = 0
    }
}