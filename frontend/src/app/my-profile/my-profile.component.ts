import { Component } from '@angular/core';
import { UserRepresentation } from '../services/api/models/user-representation';
import { Router } from '@angular/router';
import { LoginService } from '../services/api/auth/login.service';

@Component({
  selector: 'app-my-profile',
  templateUrl: './my-profile.component.html',
  styleUrls: ['./my-profile.component.scss']
})
export class MyProfileComponent {
  user:UserRepresentation = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    bio: '',
    role: ''
  };

  constructor(private router: Router,private loginService:LoginService){

  }

  login():void {
    this.loginService.loginUser(this.user).subscribe();
    this.router.navigate(['blogs']);
    
  }

  
}
